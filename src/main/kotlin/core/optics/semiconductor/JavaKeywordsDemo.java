package core.optics.semiconductor;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.GenericStyledArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;
import org.reactfx.collection.ListModification;
import org.reactfx.Subscription;

public class JavaKeywordsDemo extends Application {

  private static final String[] KEYWORDS = new String[] {
    "abstract", "assert", "boolean", "break", "byte",
    "case", "catch", "char", "class", "const",
    "continue", "default", "do", "double", "else",
    "enum", "extends", "final", "finally", "float",
    "for", "goto", "if", "implements", "import",
    "instanceof", "int", "interface", "long", "native",
    "new", "package", "private", "protected", "public",
    "return", "short", "static", "strictfp", "super",
    "switch", "synchronized", "this", "throw", "throws",
    "transient", "try", "void", "volatile", "while"
  };

  private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
  private static final String PAREN_PATTERN = "\\(|\\)";
  private static final String BRACE_PATTERN = "\\{|\\}";
  private static final String BRACKET_PATTERN = "\\[|\\]";
  private static final String SEMICOLON_PATTERN = "\\;";
  private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
  private static final String COMMENT_PATTERN = "//[^\n]*" + "|" + "/\\*(.|\\R)*?\\*/";

  private static final Pattern PATTERN = Pattern.compile(
    "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
      + "|(?<PAREN>" + PAREN_PATTERN + ")"
      + "|(?<BRACE>" + BRACE_PATTERN + ")"
      + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
      + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
      + "|(?<STRING>" + STRING_PATTERN + ")"
      + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
  );

  private static final String sampleCode = String.join("\n", new String[] {
    "package com.example;",
    "",
    "import java.util.*;",
    "",
    "public class Foo extends Bar implements Baz {",
    "",
    "    /*",
    "     * multi-line comment",
    "     */",
    "    public static void main(String[] args) {",
    "        // single-line comment",
    "        for(String arg: args) {",
    "            if(arg.length() != 0)",
    "                System.out.println(arg);",
    "            else",
    "                System.err.println(\"Warning: empty string as argument\");",
    "        }",
    "    }",
    "",
    "}"
  });


  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage primaryStage) throws MalformedURLException {
    CodeArea codeArea = new CodeArea();

    // add line numbers to the left of area
    codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
/*
        // recompute the syntax highlighting for all text, 500 ms after user stops editing area
        Subscription cleanupWhenNoLongerNeedIt = codeArea
                // plain changes = ignore style changes that are emitted when syntax highlighting is reapplied
                // multi plain changes = save computation by not rerunning the code multiple times
                //   when making multiple changes (e.g. renaming a method at multiple parts in file)
                .multiPlainChanges()
                // do not emit an event until 500 ms have passed since the last emission of previous stream
                .successionEnds(Duration.ofMillis(500))
                // run the following code block when previous stream emits an event
                .subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));
        // when no longer need syntax highlighting and wish to clean up memory leaks
        // run: `cleanupWhenNoLongerNeedIt.unsubscribe();`
*/
    // recompute syntax highlighting only for visible paragraph changes
    codeArea.getVisibleParagraphs().addModificationObserver
      (
        new VisibleParagraphStyler<>( codeArea, this::computeHighlighting )
      );

    // auto-indent: insert previous line's indents on enter
    final Pattern whiteSpace = Pattern.compile( "^\\s+" );
    codeArea.addEventHandler( KeyEvent.KEY_PRESSED, KE ->
    {
      if ( KE.getCode() == KeyCode.ENTER ) {
        int caretPosition = codeArea.getCaretPosition();
        int currentParagraph = codeArea.getCurrentParagraph();
        Matcher m0 = whiteSpace.matcher( codeArea.getParagraph( currentParagraph-1 ).getSegments().get( 0 ) );
        if ( m0.find() ) Platform.runLater( () -> codeArea.insertText( caretPosition, m0.group() ) );
      }
    });


    codeArea.replaceText(0, 0, sampleCode);

    Scene scene = new Scene(new StackPane(new VirtualizedScrollPane<>(codeArea)), 600, 400);
    scene.getStylesheets().add(new File("C:\\Users\\Decorus\\IdeaProjects\\Plasmirror\\src\\main\\kotlin\\core\\optics\\semiconductor\\java-keywords.css").toURI().toURL().toExternalForm());
    primaryStage.setScene(scene);
    primaryStage.setTitle("Java Keywords Demo");
    primaryStage.show();
  }

  private StyleSpans<Collection<String>> computeHighlighting(String text) {
    Matcher matcher = PATTERN.matcher(text);
    int lastKwEnd = 0;
    StyleSpansBuilder<Collection<String>> spansBuilder
      = new StyleSpansBuilder<>();
    while(matcher.find()) {
      String styleClass =
        matcher.group("KEYWORD") != null ? "keyword" :
          matcher.group("PAREN") != null ? "paren" :
            matcher.group("BRACE") != null ? "brace" :
              matcher.group("BRACKET") != null ? "bracket" :
                matcher.group("SEMICOLON") != null ? "semicolon" :
                  matcher.group("STRING") != null ? "string" :
                    matcher.group("COMMENT") != null ? "comment" :
                      null; /* never happens */ assert styleClass != null;
      spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
      spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
      lastKwEnd = matcher.end();
    }
    spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
    return spansBuilder.create();
  }

  private class VisibleParagraphStyler<PS, SEG, S> implements Consumer<ListModification>
  {
    private final GenericStyledArea<PS, SEG, S> area;
    private final Function<String,StyleSpans<S>> computeStyles;
    private int prevParagraph, prevTextLength;

    public VisibleParagraphStyler( GenericStyledArea<PS, SEG, S> area, Function<String,StyleSpans<S>> computeStyles )
    {
      this.computeStyles = computeStyles;
      this.area = area;
    }

    @Override
    public void accept( ListModification lm )
    {
      if ( lm.getAddedSize() > 0 )
      {
        int paragraph = Math.min( area.firstVisibleParToAllParIndex() + lm.getFrom(), area.getParagraphs().size()-1 );
        String text = area.getText( paragraph, 0, paragraph, area.getParagraphLength( paragraph ) );

        if ( paragraph != prevParagraph || text.length() != prevTextLength )
        {
          int startPos = area.getAbsolutePosition( paragraph, 0 );
          Platform.runLater( () -> area.setStyleSpans( startPos, computeStyles.apply( text ) ) );
          prevTextLength = text.length();
          prevParagraph = paragraph;
        }
      }
    }
  }

}