package io.daytrader.web;

import java.util.Set;
import org.springframework.ui.Model;

public interface ActionHandler {
    Set<String> actions();
    default boolean requiresLogin() { return true; }
    String handle(ActionRequest request, Model model) throws Exception;
}
