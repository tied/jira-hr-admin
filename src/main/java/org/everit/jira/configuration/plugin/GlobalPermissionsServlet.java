/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.jira.configuration.plugin;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.everit.expression.ExpressionCompiler;
import org.everit.expression.ParserConfiguration;
import org.everit.expression.jexl.JexlExpressionCompiler;
import org.everit.templating.CompiledTemplate;
import org.everit.templating.TemplateCompiler;
import org.everit.templating.html.HTMLTemplateCompiler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleReference;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.websudo.WebSudoManager;

/**
 * Testing the addition of a servlet.
 */
public class GlobalPermissionsServlet extends HttpServlet {

  private static final CompiledTemplate TEMPLATE;

  static {
    ExpressionCompiler expressionCompiler = new JexlExpressionCompiler();
    TemplateCompiler htmlTemplateCompiler = new HTMLTemplateCompiler(expressionCompiler);

    try {
      TEMPLATE = htmlTemplateCompiler.compile(IOUtils
          .toString(GlobalPermissionsServlet.class
              .getResource("/META-INF/pages/global_permissions.html")),
          new ParserConfiguration(GlobalPermissionsServlet.class.getClassLoader()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException {

    WebSudoManager webSudoManager =
        ComponentAccessor.getOSGiComponentInstanceOfType(WebSudoManager.class);

    if (!webSudoManager.canExecuteRequest(req)) {
      webSudoManager.enforceWebSudoProtection(req, resp);
      return;
    }

    StringWriter writer = new StringWriter();
    EveritWebResourceManager webResourceManager = new EveritWebResourceManager(writer);
    Map<String, Object> vars = new HashMap<String, Object>();
    vars.put("webResourceManager", webResourceManager);
    TEMPLATE.render(writer, vars);
    resp.getWriter().write(writer.toString());

    TransactionTemplate transactionTemplate =
        ComponentAccessor.getOSGiComponentInstanceOfType(TransactionTemplate.class);

    System.out.println(transactionTemplate);

    ClassLoader classLoader = GlobalPermissionsServlet.class.getClassLoader();
    BundleReference bundleReference = (BundleReference) classLoader;

    BundleContext bundleContext = bundleReference.getBundle().getBundleContext();
    try (FileWriter out = new FileWriter("C:/tmp/services.txt")) {
      ServiceReference<?>[] allServiceReferences =
          bundleContext.getAllServiceReferences(null, null);

      for (ServiceReference<?> serviceReference : allServiceReferences) {
        out.write(serviceReference.toString() + " - "
            + serviceReference.getBundle().getSymbolicName() + "\n");
      }
    } catch (InvalidSyntaxException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

}