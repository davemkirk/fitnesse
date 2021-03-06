// Copyright (C) 2003-2009 by Object Mentor, Inc. All rights reserved.
// Released under the terms of the CPL Common Public License version 1.0.
package fitnesse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import util.RegexTestCase;
import fitnesse.authentication.Authenticator;
import fitnesse.authentication.PromiscuousAuthenticator;
import fitnesse.html.HtmlPageFactory;
import fitnesse.responders.ResponderFactory;
import fitnesse.responders.WikiPageResponder;
import fitnesse.responders.editing.ContentFilter;
import fitnesse.responders.editing.EditResponder;
import fitnesse.responders.editing.SaveResponder;
import fitnesse.wiki.VersionsController;
import fitnesse.wiki.NullVersionsController;
import fitnesse.wiki.zip.ZipFileVersionsController;
import fitnesse.testutil.SimpleAuthenticator;
import fitnesse.wiki.FileSystemPage;
import fitnesse.wiki.InMemoryPage;
import fitnesse.wiki.WikiPage;
import fitnesse.wikitext.WidgetBuilder;
import fitnesse.wikitext.WidgetInterceptor;
import fitnesse.wikitext.WikiWidget;
import fitnesse.wikitext.widgets.BoldWidget;
import fitnesse.wikitext.widgets.ItalicWidget;
import fitnesse.wikitext.widgets.WidgetRoot;

public class ComponentFactoryTest extends RegexTestCase {
  private Properties testProperties;
  private ComponentFactory factory;

  @Override
  public void setUp() throws Exception {
    testProperties = new Properties();
    factory = new ComponentFactory(testProperties);
  }

  @Override
  public void tearDown() throws Exception {
    final File file = new File(ComponentFactory.PROPERTIES_FILE);
    FileOutputStream out = new FileOutputStream(file);
    out.write("".getBytes());
    out.close();
    TestWidgetInterceptor.widgetsIntercepted.clear();
  }

  public void testRootPageCreation() throws Exception {
    testProperties.setProperty(ComponentFactory.WIKI_PAGE_CLASS, InMemoryPage.class.getName());

    WikiPageFactory wikiPageFactory = new WikiPageFactory();
    factory.loadWikiPage(wikiPageFactory);
    assertEquals(InMemoryPage.class, wikiPageFactory.getWikiPageClass());

    WikiPage page = wikiPageFactory.makeRootPage(null, "", factory);
    assertNotNull(page);
    assertEquals(InMemoryPage.class, page.getClass());
  }

  public void testDefaultRootPage() throws Exception {
    WikiPageFactory wikiPageFactory = new WikiPageFactory();
    factory.loadWikiPage(wikiPageFactory);
    assertEquals(FileSystemPage.class, wikiPageFactory.getWikiPageClass());

    WikiPage page = wikiPageFactory.makeRootPage("testPath", "TestRoot", factory);
    assertNotNull(page);
    assertEquals(FileSystemPage.class, page.getClass());
    assertEquals("TestRoot", page.getName());
  }

  public void testDefaultHtmlPageFactory() throws Exception {
    HtmlPageFactory pageFactory = factory.getHtmlPageFactory(new HtmlPageFactory());
    assertNotNull(pageFactory);
    assertEquals(HtmlPageFactory.class, pageFactory.getClass());
  }

  public void testHtmlPageFactoryCreation() throws Exception {
    testProperties.setProperty(ComponentFactory.HTML_PAGE_FACTORY, TestPageFactory.class.getName());

    HtmlPageFactory pageFactory = factory.getHtmlPageFactory(null);
    assertNotNull(pageFactory);
    assertEquals(TestPageFactory.class, pageFactory.getClass());
  }

  public void testAddPlugins() throws Exception {
    testProperties.setProperty(ComponentFactory.PLUGINS, DummyPlugin.class.getName());

    WikiPageFactory wikiPageFactory = new WikiPageFactory();
    ResponderFactory responderFactory = new ResponderFactory(".");

    WidgetBuilder htmlWidgetBuilder = WidgetBuilder.htmlWidgetBuilder;
    WidgetBuilder.htmlWidgetBuilder = new WidgetBuilder();
    assertNull(WidgetBuilder.htmlWidgetBuilder.findWidgetClassMatching("'''text'''"));
    assertNull(WidgetBuilder.htmlWidgetBuilder.findWidgetClassMatching("''text''"));
    
    String output = factory.loadPlugins(responderFactory, wikiPageFactory);

    assertSubString(DummyPlugin.class.getName(), output);

    assertEquals(InMemoryPage.class, wikiPageFactory.getWikiPageClass());
    assertEquals(WikiPageResponder.class, responderFactory.getResponderClass("custom1"));
    assertEquals(EditResponder.class, responderFactory.getResponderClass("custom2"));
    assertEquals(BoldWidget.class, WidgetBuilder.htmlWidgetBuilder.findWidgetClassMatching("'''text'''"));
    assertEquals(ItalicWidget.class, WidgetBuilder.htmlWidgetBuilder.findWidgetClassMatching("''text''"));

    WidgetBuilder.htmlWidgetBuilder = htmlWidgetBuilder;
  }

  public void testAddResponderPlugins() throws Exception {
    String respondersValue = "custom1:" + WikiPageResponder.class.getName() + ",custom2:" + EditResponder.class.getName();
    testProperties.setProperty(ComponentFactory.RESPONDERS, respondersValue);

    ResponderFactory responderFactory = new ResponderFactory(".");
    String output = factory.loadResponders(responderFactory);

    assertSubString("custom1:" + WikiPageResponder.class.getName(), output);
    assertSubString("custom2:" + EditResponder.class.getName(), output);

    assertEquals(WikiPageResponder.class, responderFactory.getResponderClass("custom1"));
    assertEquals(EditResponder.class, responderFactory.getResponderClass("custom2"));
  }

  public void testWikiWidgetPlugins() throws Exception {
    String widgetsValue = BoldWidget.class.getName() + ", " + ItalicWidget.class.getName();
    testProperties.setProperty(ComponentFactory.WIKI_WIDGETS, widgetsValue);

    String output = factory.loadWikiWidgets();

    assertSubString(BoldWidget.class.getName(), output);
    assertSubString(ItalicWidget.class.getName(), output);

    assertEquals(BoldWidget.class, WidgetBuilder.htmlWidgetBuilder.findWidgetClassMatching("'''text'''"));
    assertEquals(ItalicWidget.class, WidgetBuilder.htmlWidgetBuilder.findWidgetClassMatching("''text''"));
  }

  public void testWikiWidgetInterceptors() throws Exception {
    testProperties.setProperty(ComponentFactory.WIKI_WIDGET_INTERCEPTORS, TestWidgetInterceptor.class.getName());

    String output = factory.loadWikiWidgetInterceptors();

    assertSubString(TestWidgetInterceptor.class.getName(), output);

    new WidgetRoot("hello '''world'''" + "\n", (WikiPage) null, WidgetBuilder.htmlWidgetBuilder);
    assertTrue(TestWidgetInterceptor.widgetsIntercepted.contains(BoldWidget.class));
  }

  public static class TestWidgetInterceptor implements WidgetInterceptor {
    public static List<Class<?>> widgetsIntercepted = new ArrayList<Class<?>>();

    public void intercept(WikiWidget widget) {
      widgetsIntercepted.add(widget.getClass());
    }
  }

  public void testAuthenticatorDefaultCreation() throws Exception {
    Authenticator authenticator = factory.getAuthenticator(new PromiscuousAuthenticator());
    assertNotNull(authenticator);
    assertEquals(PromiscuousAuthenticator.class, authenticator.getClass());
  }

  public void testAuthenticatorCustomCreation() throws Exception {
    testProperties.setProperty(ComponentFactory.AUTHENTICATOR, SimpleAuthenticator.class.getName());

    Authenticator authenticator = factory.getAuthenticator(new PromiscuousAuthenticator());
    assertNotNull(authenticator);
    assertEquals(SimpleAuthenticator.class, authenticator.getClass());
  }

  public void testContentFilterCreation() throws Exception {
    assertEquals("", factory.loadContentFilter());
    assertEquals(null, SaveResponder.contentFilter);

    testProperties.setProperty(ComponentFactory.CONTENT_FILTER, TestContentFilter.class.getName());

    String content = factory.loadContentFilter();
    assertEquals("\tContent filter installed: " + SaveResponder.contentFilter.getClass().getName() + "\n", content);
    assertNotNull(SaveResponder.contentFilter);
    assertEquals(TestContentFilter.class, SaveResponder.contentFilter.getClass());
  }

  public void testShouldUseZipFileRevisionControllerAsDefault() throws Exception {
    VersionsController defaultRevisionController = factory.loadVersionsController();
    assertEquals(ZipFileVersionsController.class, defaultRevisionController.getClass());
  }

  public void testShouldUseSpecifiedRevisionController() throws Exception {
    testProperties.setProperty(ComponentFactory.VERSIONS_CONTROLLER, NullVersionsController.class.getName());

    VersionsController defaultRevisionController = factory.loadVersionsController();
    assertEquals(NullVersionsController.class, defaultRevisionController.getClass());
  }

  public static class TestPageFactory extends HtmlPageFactory {
    public TestPageFactory(Properties p) {
      p.propertyNames();
    }
  }

  public static class TestContentFilter implements ContentFilter {
    public TestContentFilter(Properties p) {
      p.propertyNames();
    }

    public boolean isContentAcceptable(String content, String page) {
      return false;
    }
  }

  static class DummyPlugin {
    public static void registerWikiPage(WikiPageFactory factory) {
      factory.setWikiPageClass(InMemoryPage.class);
    }

    public static void registerResponders(ResponderFactory factory) {
      factory.addResponder("custom1", WikiPageResponder.class);
      factory.addResponder("custom2", EditResponder.class);
    }

    public static void registerWikiWidgets(WidgetBuilder builder) {
      builder.addWidgetClass(BoldWidget.class);
      builder.addWidgetClass(ItalicWidget.class);
    }
  }
}
