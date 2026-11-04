#!/usr/bin/env python3
"""Scan the repository for WebSphere/Java EE container dependencies and write docs/WEBSPHERE_INVENTORY.md.

Sources: ibm-*.xml bindings/extensions, Java EE descriptors, Java annotations and JNDI lookups,
EJBs and MDBs, Liberty server.xml features and resources, com.ibm.* imports, and the synthetic
wsadmin scripts in legacy-ops/. Each row is mapped to a proposed Spring Boot / AWS replacement.
Run: python3 tools/ws_inventory.py [--check]
"""
import argparse
import os
import re
import sys
import xml.etree.ElementTree as ET

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUTPUT = os.path.join(REPO, "docs", "WEBSPHERE_INVENTORY.md")
SKIP_DIRS = {".git", "target", "build", ".venv", "node_modules", "work", "__pycache__", ".gradle",
             "daytrader-boot", "infra"}
PROJECT_PACKAGE = "com.ibm.websphere.samples.daytrader"

REPLACEMENTS = [
    (r"IBM binding/extension file", r"^ibm-ejb-jar-bnd\.xml$",
     "Deleted; EJB bindings become Spring bean wiring (@Service, @SqsListener) and application.yml."),
    (r"IBM binding/extension file", r"^ibm-web-bnd\.xml$",
     "Deleted; resource refs become injected beans and the virtual host becomes an ALB listener."),
    (r"IBM binding/extension file", r"^ibm-web-ext\.xml$",
     "Deleted; preserve isolation with spring.datasource.hikari.transaction-isolation=TRANSACTION_READ_COMMITTED."),
    (r".*", r"^TradeDataSourceAuthData$",
     "AWS Secrets Manager secret for Aurora-managed master credentials, injected as ECS task secrets "
     "SPRING_DATASOURCE_USERNAME/PASSWORD."),
    (r".*", r"^TradeAdminAuthData$",
     "ECS task IAM role for SQS/SNS access; no static credentials."),
    (r".*", r"jdbc/|DataSource",
     "spring.datasource.* with HikariCP on Aurora PostgreSQL; inject the DataSource/repositories. "
     "NoTx uses the same pool, with Spring managing non-transactional reads."),
    (r".*", r"QueueConnectionFactory|TradeBrokerQCF|TradeBrokerQueue|TradeBrokerMDB|DTBroker3MDB",
     "Amazon SQS queue daytrader-broker: SqsTemplate send and @SqsListener in TradeBrokerListener "
     "(Spring Cloud AWS 3.x), profile sqs and off by default; synchronous orders remain the default."),
    (r".*", r"TopicConnectionFactory|TradeStreamerTCF|TradeStreamerTopic|TradeStreamerMDB|DTStreamer3MDB",
     "Amazon SNS topic daytrader-streamer fanned out to SQS; @SqsListener in TradeStreamerListener "
     "(Spring Cloud AWS 3.x), profile sqs and off by default."),
    (r".*", r"DirectSLSBBean",
     "Dropped: the Direct (JDBC) runtime mode is not ported; retain the JPA path (decision in MIGRATION_PLAN.md)."),
    (r".*", r"TradeSLSBBean",
     "Constructor-injected TradeService (@Service, @Transactional)."),
    (r".*", r"java:comp/UserTransaction",
     "@Transactional with JpaTransactionManager."),
    (r".*", r"openejb:",
     "Removed; OpenEJB container-specific fallback lookup is not used."),
    (r".*", r"default_host",
     "Embedded Tomcat server.port=8080 behind an ALB listener."),
    (r".*", r"reloadEnabled|reloadInterval",
     "spring.thymeleaf.cache controls template caching."),
    (r"JPA persistence unit", r".*",
     "Spring Data JPA auto-configuration (Hibernate 6 / Jakarta Persistence 3.1), spring.jpa.*, same tables."),
    (r"EAR module", r"^web\.war$",
     "Spring Boot fat jar with server.servlet.context-path=/daytrader."),
    (r"EAR module", r"^Rest\.war$",
     "@RestController in the same jar; /rest/** routed by Tomcat RewriteValve and an ALB path rule."),
    (r"EAR module", r"^dt-ejb\.jar$",
     "io.daytrader.service and io.daytrader.entity packages in the Spring Boot jar."),
    (r"Liberty feature", r"^ejbLite-3\.1$",
     "spring-context services with @Service and @Transactional."),
    (r"Liberty feature", r"^jaxrs-1\.1$",
     "spring-boot-starter-web."),
    (r"Liberty feature", r"^jmsMdb-3\.1$|^wasJmsClient-1\.1$",
     "spring-cloud-aws-starter-sqs (+ SNS), profile sqs; synchronous order processing is the default."),
    (r"Liberty feature", r"^jpa-2\.0$",
     "spring-boot-starter-data-jpa."),
    (r"Liberty feature", r"^jsf-2\.0$",
     "Dropped; port the UI to spring-boot-starter-thymeleaf."),
    (r"Liberty feature", r"^wasJmsServer-1\.0$",
     "Managed Amazon SQS/SNS; no embedded broker."),
    (r"Liberty server config", r"^DerbyLib$",
     "org.postgresql:postgresql driver dependency (H2 for local/tests)."),
    (r"Liberty server config", r"^daytrader3$",
     "ECS Fargate service."),
    (r"Liberty server config", r"^defaultHttpEndpoint$",
     "Embedded server.port=8080 behind the ALB; TLS terminates at the ALB."),
    (r"Liberty server config", r"^defaultME$",
     "Managed Amazon SQS/SNS resources."),
    (r"com\.ibm\.\* import", r".*",
     "Project-internal import; ported classes move to io.daytrader.* (the legacy package name is not a WebSphere API)."),
    (r"wsadmin resource", r"^DB2$",
     "PostgreSQL JDBC driver dependency."),
    (r"wsadmin resource", r"^DB2UNIVERSAL_JDBC_DRIVER_PATH$",
     "Not needed; the PostgreSQL JDBC driver is packaged inside the fat jar."),
    (r"wsadmin resource", r"^DayTraderSharedLib$",
     "Maven dependencies packaged in the Spring Boot fat jar."),
    (r"wsadmin resource", r"^daytrader3$",
     "ECS task definition and service with an ALB target group."),
]


def repoFiles():
    for root, dirs, files in os.walk(REPO):
        dirs[:] = sorted(d for d in dirs if d not in SKIP_DIRS)
        # the committed copy of application.xml under the Liberty apps dir is a build output
        for name in sorted(files):
            yield os.path.relpath(os.path.join(root, name), REPO)


def local(tag):
    return tag.split("}", 1)[-1]


def parseXml(path):
    try:
        return ET.parse(os.path.join(REPO, path)).getroot()
    except ET.ParseError as exc:
        sys.exit(f"cannot parse {path}: {exc}")


def text(element, childName):
    for child in element:
        if local(child.tag) == childName:
            return (child.text or "").strip()
    return ""


class Inventory:
    def __init__(self):
        self.rows = []

    def add(self, category, item, source, detail=""):
        row = (category, item, source, detail)
        if row not in self.rows:
            self.rows.append(row)


def scanIbmXml(inv, path):
    root = parseXml(path)
    inv.add("IBM binding/extension file", os.path.basename(path), path, f"root <{local(root.tag)}>")
    owner = ""
    for element in root.iter():
        tag = local(element.tag)
        attrs = element.attrib
        if tag in ("session", "message-driven"):
            owner = attrs.get("name", "")
        name = attrs.get("name", "")
        binding = attrs.get("binding-name", "")
        if tag in ("resource-ref", "message-destination-ref", "resource-env-ref", "ejb-ref", "message-destination"):
            scoped = owner and "ejb-jar" in path and tag != "message-destination"
            detail = f"<{tag}>" + (f" in {owner}" if scoped else "")
            if binding:
                detail += f" -> binding {binding}"
            if "isolation-level" in attrs:
                detail += f", isolation {attrs['isolation-level']}"
            inv.add("IBM binding", name, path, detail)
        elif tag == "authentication-alias":
            inv.add("J2C auth alias reference", name, path, "<authentication-alias>")
        elif tag == "jca-adapter":
            inv.add("MDB activation binding", owner, path,
                    f"activation spec {attrs.get('activation-spec-binding-name')} -> destination "
                    f"{attrs.get('destination-binding-name')}, auth alias {attrs.get('activation-spec-auth-alias')}")
        elif tag == "virtual-host":
            inv.add("IBM binding", name, path, "<virtual-host>")
        elif tag in ("jsp-attribute", "file-serving-attribute", "reload-interval", "enable-serving-servlets-by-class-name"):
            inv.add("IBM extension setting", name or tag, path, f"<{tag}> {attrs.get('value', '')}".strip())


def scanDescriptor(inv, path):
    root = parseXml(path)
    kind = os.path.basename(path)
    for element in root.iter():
        tag = local(element.tag)
        if tag in ("resource-ref", "resource-env-ref", "message-destination-ref"):
            name = text(element, "res-ref-name") or text(element, "resource-env-ref-name") \
                or text(element, "message-destination-ref-name")
            kind_ = text(element, "res-type") or text(element, "resource-env-ref-type") \
                or text(element, "message-destination-type")
            inv.add("JNDI reference", name, path, f"<{tag}> {kind_}".strip())
        elif tag == "ejb-local-ref" or tag == "ejb-ref":
            inv.add("JNDI reference", text(element, "ejb-ref-name"), path, f"<{tag}> {text(element, 'local')}".strip())
        elif tag in ("session", "message-driven") and kind == "ejb-jar.xml":
            category = "MDB" if tag == "message-driven" else "EJB"
            inv.add(category, text(element, "ejb-name"), path,
                    f"ejb-jar.xml {text(element, 'session-type') or tag} {text(element, 'ejb-class')}".strip())
        elif tag == "persistence-unit":
            inv.add("JPA persistence unit", element.attrib.get("name", ""), path,
                    f"jta {text(element, 'jta-data-source')}, non-jta {text(element, 'non-jta-data-source')}")
        elif tag == "message-destination" and kind == "ejb-jar.xml":
            inv.add("MDB destination", text(element, "message-destination-name"), path, "ejb-jar.xml <message-destination>")
        elif tag == "web" and kind == "application.xml":
            inv.add("EAR module", text(element, "web-uri"), path, f"context root {text(element, 'context-root')}")
        elif tag == "ejb" and kind == "application.xml":
            inv.add("EAR module", (element.text or "").strip(), path, "EJB module")
        elif tag == "security-constraint":
            inv.add("Web security constraint", text(element, "display-name") or "security-constraint", path, "web.xml")


def scanServerXml(inv, path):
    root = parseXml(path)
    for element in root.iter():
        tag = local(element.tag)
        attrs = element.attrib
        if tag == "feature":
            inv.add("Liberty feature", (element.text or "").strip(), path, "<featureManager>")
        elif tag in ("dataSource", "jmsQueueConnectionFactory", "jmsTopicConnectionFactory", "jmsQueue", "jmsTopic",
                     "jmsConnectionFactory"):
            inv.add("JNDI binding (Liberty)", attrs.get("jndiName", attrs.get("id", "")), path, f"<{tag}>")
        elif tag == "jmsActivationSpec":
            props = next((c.attrib for c in element if local(c.tag).startswith("properties")), {})
            inv.add("JNDI binding (Liberty)", attrs.get("id", ""), path,
                    f"<jmsActivationSpec> destination {props.get('destinationRef', '')}")
        elif tag == "authData":
            inv.add("J2C auth alias (Liberty)", attrs.get("id", ""), path, "<authData>")
        elif tag in ("library", "messagingEngine", "application", "httpEndpoint", "transaction"):
            inv.add("Liberty server config", attrs.get("id", attrs.get("name", tag)), path, f"<{tag}>")


ANNOTATION = re.compile(r"@(Stateless|Stateful|Singleton|MessageDriven)\b")
CLASS = re.compile(r"\bclass\s+(\w+)")
RESOURCE = re.compile(r"@Resource\s*\(([^)]*)\)")
NAME_ATTR = re.compile(r"\b(?:name|mappedName|lookup)\s*=\s*\"([^\"]+)\"")
LOOKUP = re.compile(r"\.lookup\(\s*\"([^\"]+)\"")
ACTIVATION = re.compile(r"propertyName\s*=\s*\"(destination|destinationType)\"\s*,\s*propertyValue\s*=\s*\"([^\"]+)\"")
IMPORT = re.compile(r"^import\s+(?:static\s+)?(com\.ibm\.[\w.*]+)\s*;", re.M)
CONSTANT_JNDI = re.compile(r"\"(java:comp/[^\"]+|jdbc/[^\"]+|jms/[^\"]+|eis/[^\"]+)\"")


def scanJava(inv, path, imports):
    source = open(os.path.join(REPO, path), encoding="utf-8", errors="replace").read()
    className = (CLASS.search(source) or [None, os.path.basename(path)[:-5]])[1]
    for match in ANNOTATION.finditer(source):
        kind = match.group(1)
        if kind == "MessageDriven":
            props = dict(ACTIVATION.findall(source))
            inv.add("MDB", className, path, f"@MessageDriven destination {props.get('destination', '?')} "
                                             f"({props.get('destinationType', '?')})")
            if props.get("destination"):
                inv.add("MDB destination", props["destination"], path, f"consumed by {className}")
        else:
            inv.add("EJB", className, path, f"@{kind}")
    for match in RESOURCE.finditer(source):
        names = NAME_ATTR.findall(match.group(1))
        if names:
            inv.add("JNDI reference", names[0], path, f"@Resource in {className}")
    for name in LOOKUP.findall(source):
        inv.add("JNDI lookup", name, path, f"InitialContext.lookup in {className}")
    for name in CONSTANT_JNDI.findall(source):
        inv.add("JNDI name constant", name, path, f"string literal in {className}")
    for name in IMPORT.findall(source):
        imports.setdefault(name, set()).add(path)


WSADMIN_CALL = re.compile(r"(AdminTask|AdminConfig|AdminApp)\.(\w+)\(")
OPTION = re.compile(r"-(name|jndiName|alias|type|destinationJndiName|queueName|topicName|variableName)\s+\"?([^\s\"\]]+)")


def scanWsadmin(inv, path):
    source = open(os.path.join(REPO, path), encoding="utf-8").read()
    # join each statement (calls can span several lines) up to the next top-level call
    for match in WSADMIN_CALL.finditer(source):
        obj, verb = match.groups()
        if verb in ("save", "getid", "showAttribute", "modify", "queryNames", "invoke"):
            continue
        end = source.find("\n\n", match.end())
        statement = source[match.start(): end if end != -1 else len(source)]
        options = dict((k, v) for k, v in OPTION.findall(statement))
        if obj == "AdminApp" and verb == "install":
            inv.add("wsadmin resource", "daytrader3", path, "AdminApp.install (resource-ref, MDB and shared-library mappings)")
            continue
        if verb == "create":
            lib = re.search(r"\['name',\s*'([^']+)'\]", statement)
            inv.add("wsadmin resource", lib.group(1) if lib else "?", path, "AdminConfig.create Library (shared library)")
            continue
        item = options.get("jndiName") or options.get("alias") or options.get("name") or options.get("variableName") or "?"
        detail = f"{obj}.{verb}"
        for key in ("type", "destinationJndiName", "queueName", "topicName"):
            if key in options:
                detail += f" {key}={options[key]}"
        inv.add("wsadmin resource", item, path, detail)


def build():
    inv, imports = Inventory(), {}
    for path in repoFiles():
        name = os.path.basename(path)
        if "/apps/META-INF/" in path:
            continue
        if re.match(r"ibm-.*\.xml$", name):
            scanIbmXml(inv, path)
        elif name in ("web.xml", "ejb-jar.xml", "persistence.xml", "application.xml"):
            scanDescriptor(inv, path)
        elif name.startswith("server") and name.endswith(".xml") and "wlpcfg" in path:
            scanServerXml(inv, path)
        elif name.endswith(".java") and "/src/main/" in path:
            scanJava(inv, path, imports)
        elif path.startswith("legacy-ops/wsadmin/") and name.endswith(".py"):
            scanWsadmin(inv, path)
    for name, paths in sorted(imports.items()):
        note = "project package (not a WebSphere API)" if name.startswith(PROJECT_PACKAGE) else "IBM API"
        inv.add("com.ibm.* import", name, f"{len(paths)} file(s)", note)
    return inv.rows


ORDER = ["IBM binding/extension file", "IBM binding", "IBM extension setting", "J2C auth alias reference",
         "MDB activation binding", "JNDI reference", "JNDI lookup", "JNDI name constant", "JPA persistence unit",
         "EAR module", "Web security constraint", "EJB", "MDB", "MDB destination", "Liberty feature",
         "JNDI binding (Liberty)", "J2C auth alias (Liberty)", "Liberty server config", "com.ibm.* import",
         "wsadmin resource"]


def render(rows):
    rows = sorted(rows, key=lambda r: (ORDER.index(r[0]) if r[0] in ORDER else 99, r[2], r[1], r[3]))
    counts = {}
    for row in rows:
        counts[row[0]] = counts.get(row[0], 0) + 1
    ibmApis = sum(1 for r in rows if r[0] == "com.ibm.* import" and r[3] == "IBM API")
    out = [
        "# WebSphere dependency inventory",
        "",
        "<!-- Generated by tools/ws_inventory.py. Do not edit by hand; run `make inventory`. -->",
        "",
        f"**{len(rows)} rows.** Every row has a proposed Spring Boot / AWS replacement "
        "(mapping table in `tools/ws_inventory.py`; see `docs/MIGRATION_PLAN.md`).",
        "",
        f"- `com.ibm.*` imports that are IBM APIs (outside the `{PROJECT_PACKAGE}` package): **{ibmApis}**. "
        "The app's own package is named `com.ibm.websphere.*`; that is a name, not a dependency on WebSphere-proprietary APIs.",
        "- `legacy-ops/wsadmin` rows come from **synthetic** tWAS scripts (see `legacy-ops/README.md`). "
        "They describe the estate the migration must translate, not this repo's Liberty runtime.",
        "",
        "| Category | Rows |",
        "| --- | --- |",
        *[f"| {c} | {counts[c]} |" for c in ORDER if c in counts],
        "",
        "| # | Category | Item | Source | Detail | Proposed Spring Boot / AWS replacement |",
        "| --- | --- | --- | --- | --- | --- |",
    ]
    for i, row in enumerate(rows, 1):
        category, item, source, detail = row
        cells = [str(i), category, f"`{item}`", f"`{source}`" if "/" in source else source, detail,
                 replacementFor(row)]
        out.append("| " + " | ".join(c.replace("|", "\\|") for c in cells) + " |")
    return "\n".join(out) + "\n"


def replacementFor(row):
    category, item, source, detail = row
    for categoryPattern, itemPattern, replacement in REPLACEMENTS:
        if re.search(categoryPattern, category) and re.search(itemPattern, item):
            return replacement
    sys.exit(f"no migration replacement for inventory row: {category} / {item} ({source}: {detail})")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="fail if docs/WEBSPHERE_INVENTORY.md is out of date")
    args = parser.parse_args()
    content = render(build())
    if args.check:
        current = open(OUTPUT).read() if os.path.exists(OUTPUT) else ""
        if current != content:
            sys.exit("docs/WEBSPHERE_INVENTORY.md is stale: run make inventory")
        print("inventory up to date")
        return
    with open(OUTPUT, "w") as handle:
        handle.write(content)
    print(f"wrote {os.path.relpath(OUTPUT)} ({content.count(chr(10)) } lines)")


if __name__ == "__main__":
    main()
