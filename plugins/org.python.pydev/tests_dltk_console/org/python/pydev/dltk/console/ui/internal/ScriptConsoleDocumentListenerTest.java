/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.dltk.console.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.console.IConsoleLineTracker;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.autoedit.PyAutoIndentStrategy;
import org.python.pydev.core.autoedit.TestIndentPrefs;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_interactive_console.console.InterpreterResponse;
import org.python.pydev.shared_interactive_console.console.ScriptConsoleHistory;
import org.python.pydev.shared_interactive_console.console.ScriptConsolePrompt;
import org.python.pydev.shared_interactive_console.console.ui.IConsoleStyleProvider;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleSession;
import org.python.pydev.shared_interactive_console.console.ui.internal.ICommandHandler;
import org.python.pydev.shared_interactive_console.console.ui.internal.IScriptConsoleViewer2ForDocumentListener;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsoleDocumentListener;

import junit.framework.TestCase;

public class ScriptConsoleDocumentListenerTest extends TestCase {

    private IDocument doc;
    private ScriptConsoleDocumentListener listener;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.doc = new Document();
        final List<String> commandsHandled = new ArrayList<String>();

        ScriptConsolePrompt prompt = new ScriptConsolePrompt(">>> ", "... ");
        listener = new ScriptConsoleDocumentListener(
                new IScriptConsoleViewer2ForDocumentListener() {

                    @Override
                    public IDocument getDocument() {
                        return doc;
                    }

                    @Override
                    public IConsoleStyleProvider getStyleProvider() {
                        return null;
                    }

                    @Override
                    public void revealEndOfDocument() {
                        //do nothing
                    }

                    @Override
                    public void setCaretOffset(int length, boolean async) {
                        //do nothing
                    }

                    @Override
                    public int getCommandLineOffset() {
                        return 0;
                    }

                    @Override
                    public int getConsoleWidthInCharacters() {
                        return 0;
                    }

                    @Override
                    public int getCaretOffset() {
                        return 0;
                    }

                    @Override
                    public IScriptConsoleSession getConsoleSession() {
                        return null;
                    }
                },

                new ICommandHandler() {

                    @Override
                    public void beforeHandleCommand(String userInput,
                            ICallback<Object, InterpreterResponse> onResponseReceived) {
                        commandsHandled.add(userInput);
                    }

                    @Override
                    public void handleCommand(String userInput,
                            ICallback<Object, InterpreterResponse> onResponseReceived) {
                        boolean more = false;
                        if (userInput.endsWith(":") || userInput.endsWith("\\")) {
                            more = true;
                        }
                        onResponseReceived.call(new InterpreterResponse(more, false));
                    }

                    @Override
                    public ICompletionProposalHandle[] getTabCompletions(String commandLine, int cursorPosition) {
                        return null;
                    }

                    @Override
                    public void setOnContentsReceivedCallback(
                            ICallback<Object, Tuple<String, String>> onContentsReceived) {
                    }

                    @Override
                    public boolean isOnStateWhereCommandHandlingShouldStop(String commandLine) {
                        return false;
                    }

                },

                prompt, new ScriptConsoleHistory(), new ArrayList<IConsoleLineTracker>(), "",
                new PyAutoIndentStrategy(new IAdaptable() {

                    @Override
                    public <T> T getAdapter(Class<T> adapter) {
                        return null;
                    }
                }));

        PyAutoIndentStrategy strategy = (PyAutoIndentStrategy) listener.getIndentStrategy();
        strategy.setIndentPrefs(new TestIndentPrefs(true, 4));
        listener.setDocument(doc);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testConsoleListener() throws Exception {
        doc.replace(0, 0, ">>> class A:");
        doc.replace(doc.getLength(), 0, "\n");
        //Things happen in a thread now, so, we have to wait for it to happen...
        for (int i = 0; i < 50; i++) {
            //if we get at the expected condition, break our for.
            if (StringUtils.format(">>> class A:%s>>>     ", listener.getDelimeter()).equals(doc.get())) {
                break;
            }
            synchronized (this) {
                wait(250);
            }
        }
        assertEquals(StringUtils.format(">>> class A:%s>>>     ", listener.getDelimeter()), doc.get());
        doc.replace(doc.getLength(), 0, "def m1");
        doc.replace(doc.getLength(), 0, "(");
        assertEquals(StringUtils.format(">>> class A:%s>>>     def m1(self):", listener.getDelimeter()), doc.get());

        listener.clear(false);
        assertEquals(">>> ", doc.get());
        doc.replace(doc.getLength(), 0, "c()");
        assertEquals(">>> c()", doc.get());
        doc.replace(doc.getLength() - 1, 0, ")");
        assertEquals(">>> c()", doc.get());
        doc.replace(doc.getLength(), 0, ")");
        assertEquals(">>> c())", doc.get());

        doc.replace(doc.getLength() - 4, 4, "");
        assertEquals(">>> ", doc.get());

        doc.replace(doc.getLength(), 0, "tttbbb");
        assertEquals(">>> tttbbb", doc.get());

        doc.replace(doc.getLength() - 3, 0, "(");
        assertEquals(">>> ttt(bbb", doc.get());

        doc.replace(doc.getLength() - 4, 1, "");
        assertEquals(">>> tttbbb", doc.get());

        doc.replace(doc.getLength(), 0, "(");
        assertEquals(">>> tttbbb()", doc.get());

    }
}
