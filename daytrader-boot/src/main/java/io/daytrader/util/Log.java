/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.daytrader.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Log {
    private static final Logger LOGGER = LoggerFactory.getLogger("io.daytrader");
    private static volatile boolean trace;

    private Log() { }
    public static boolean doTrace() { return trace; }
    public static void setTrace(boolean enabled) { trace = enabled; }
    public static void log(String message) { LOGGER.info(message); }
    public static void trace(String message) { if (trace) LOGGER.debug(message); }
    public static void trace(String message, Object... values) { if (trace) LOGGER.debug(message + " {}", java.util.Arrays.toString(values)); }
    public static void error(String message, Throwable error) { LOGGER.error(message, error); }
    public static void error(Throwable error, String message) { LOGGER.error(message, error); }
    public static void error(String message) { LOGGER.error(message); }
}
