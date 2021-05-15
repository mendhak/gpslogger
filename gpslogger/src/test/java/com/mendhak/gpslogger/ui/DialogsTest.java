package com.mendhak.gpslogger.ui;

import androidx.test.filters.SmallTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


@SmallTest
@RunWith(MockitoJUnitRunner.class)
public class DialogsTest {

    @Test
    public void GetFormattedErrorMessageForDisplay_WithMessageAndThrowable() {
        String message = "An error occurred.\r\n220 ----------------- There was a problem\r\nNo username specified.\r\nServer not configured properly.";
        Throwable t = new Throwable("Parameter not set.\r\nException handling not configured.\r\nEat oranges they are great.\r\n");
        t.setStackTrace(new StackTraceElement[]{new StackTraceElement("XXX", "yyy", "BBB", 99)});

        String expected =  "<b>An error occurred.<br />220 ----------------- There was a problem<br />No username specified.<br />Server not configured properly.</b> <br /><br />Parameter not set.<br />Exception handling not configured.<br />Eat oranges they are great.<br /><br /><br />";
        assertThat("Message formatted for error display", Dialogs.getFormattedErrorMessageForDisplay(message, t), is(expected));

    }

    @Test
    public void GetFormattedErrorMessageForDisplay_WithMessageOrThrowableMissing(){
        String message = "An error occurred.\r\n220 ----------------- There was a problem\r\nNo username specified.\r\nServer not configured properly.";
        String expected = "<b>An error occurred.<br />220 ----------------- There was a problem<br />No username specified.<br />Server not configured properly.</b> <br /><br />";
        assertThat("Message formatted even without exception object", Dialogs.getFormattedErrorMessageForDisplay(message, null), is(expected));

        expected = "";
        assertThat("Null message handled gracefully", Dialogs.getFormattedErrorMessageForDisplay(null, null), is(expected));

        Throwable t = new Throwable((String)null);
        assertThat("Null message handled gracefully", Dialogs.getFormattedErrorMessageForDisplay(null, t), is(expected));
    }

    @Test
    public void GetFormattedErrorMessageForDisplay_NewLinesReplaced(){
        String message = "An error occurred.\r\nThis is the next line.";
        String expected = "<b>An error occurred.<br />This is the next line.</b> <br /><br />";
        assertThat("Backslash r backslash n replaced", Dialogs.getFormattedErrorMessageForDisplay(message, null), is(expected));

        message = "An error occurred.\nThis is the next line.";
        assertThat("Backslash n replaced", Dialogs.getFormattedErrorMessageForDisplay(message, null), is(expected));

    }

    @Test
    public void GetFormattedErrorMessageForPlainText_WithMessageAndThrowable() {
        String message = "An error occurred.\r\n220 ----------------- There was a problem\r\nNo username specified.\r\nServer not configured properly.";
        Throwable t = new Throwable("Parameter not set.\r\nException handling not configured.\r\nEat oranges they are great.\r\n");
        t.setStackTrace(new StackTraceElement[]{new StackTraceElement("XXX", "yyy", "BBB", 99)});

        String plainText = "An error occurred.\r\n220 ----------------- There was a problem\r\nNo username specified.\r\nServer not configured properly.\r\n\r\n\r\nParameter not set.\r\nException handling not configured.\r\nEat oranges they are great.\r\n\r\njava.lang.Throwable: Parameter not set.\r\nException handling not configured.\r\nEat oranges they are great.\r\n\n\tat XXX.yyy(BBB:99)\n";
        assertThat("Message formatted for emailing copying", Dialogs.getFormattedErrorMessageForPlainText(message, t), is(plainText));
    }

    @Test
    public void GetFormattedErrorMessageForPlainText_WithMessageOnly() {
        String message = "An error occurred.\r\n220 ----------------- There was a problem\r\nNo username specified.\r\nServer not configured properly.";
        String plainText = "An error occurred.\r\n220 ----------------- There was a problem\r\nNo username specified.\r\nServer not configured properly.\r\n\r\n\r\nThis is a detailed message\r\n";
        Throwable t = new Throwable("This is a detailed message");
        t.setStackTrace(new StackTraceElement[0]);
        assertThat("Message formatted without stacktrace fully present", Dialogs.getFormattedErrorMessageForPlainText(message, t), is(plainText));

    }

    @Test
    public void GetFormattedErrorMessageForPlainText_WithMessageOrThrowableMissing(){
        String message = "An error occurred.\r\n220 ----------------- There was a problem\r\nNo username specified.\r\nServer not configured properly.";


        String plainText = "An error occurred.\r\n220 ----------------- There was a problem\r\nNo username specified.\r\nServer not configured properly.\r\n";
        assertThat("Message formatted without stacktrace fully present", Dialogs.getFormattedErrorMessageForPlainText(message, null), is(plainText));

        plainText = "";
        assertThat("Null message handled gracefully", Dialogs.getFormattedErrorMessageForPlainText(null, null), is(plainText));

        Throwable t = new Throwable((String)null);
        t.setStackTrace(new StackTraceElement[0]);
        assertThat("Null message handled gracefully", Dialogs.getFormattedErrorMessageForPlainText(null, t), is(plainText));
    }

}