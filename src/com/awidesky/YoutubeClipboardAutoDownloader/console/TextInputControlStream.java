/**
 * Copyright (C) 2015 uphy.jp
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.awidesky.YoutubeClipboardAutoDownloader.console;

import javafx.application.Platform;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;


/**
 * @author Yuhi Ishikura
 */
class TextInputControlStream {

  private final TextInputControlInputStream in;
  private final TextInputControlOutputStream out;
  private final Charset charset;

  TextInputControlStream(final TextInputControl textInputControl, Charset charset) {
    this.charset = charset;
    this.in = new TextInputControlInputStream(textInputControl);
    this.out = new TextInputControlOutputStream(textInputControl);

    textInputControl.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
      if (e.getCode() == KeyCode.ENTER) {
        getIn().enterKeyPressed();
        return;
      }

      if (textInputControl.getCaretPosition() <= getIn().getLastLineBreakIndex()) {
        e.consume();
      }
    });
    textInputControl.addEventFilter(KeyEvent.KEY_TYPED, e -> {
      if (textInputControl.getCaretPosition() < getIn().getLastLineBreakIndex()) {
        e.consume();
      }
    });
  }

  void clear() throws IOException {
    this.in.clear();
    this.out.clear();
  }

  TextInputControlInputStream getIn() {
    return this.in;
  }

  TextInputControlOutputStream getOut() {
    return this.out;
  }

  void startProgramInput() {
    // do nothing
  }

  void endProgramInput() {
    getIn().moveLineStartToEnd();
  }

  Charset getCharset() {
    return this.charset;
  }

  /**
   * @author Yuhi Ishikura
   */
  class TextInputControlInputStream extends InputStream {

    private final TextInputControl textInputControl;
    private final PipedInputStream outputTextSource;
    private final PipedOutputStream inputTextTarget;
    private int lastLineBreakIndex = 0;

    /**
     * {@link TextInputControlInputStream}�궕�깣�궦�궒�궚�깉�굮礪뗧칹�걮�겲�걲��
     *
     * @param textInputControl �뀯�뒟�뀇�겗�깇�궘�궧�깉�궠�꺍�깮�꺖�깓�꺍�깉
     */
    public TextInputControlInputStream(TextInputControl textInputControl) {
      this.textInputControl = textInputControl;
      this.inputTextTarget = new PipedOutputStream();
      try {
        this.outputTextSource = new PipedInputStream(this.inputTextTarget);
      } catch (IOException e1) {
        throw new RuntimeException(e1);
      }
    }

    int getLastLineBreakIndex() {
      return this.lastLineBreakIndex;
    }

    void moveLineStartToEnd() {
      this.lastLineBreakIndex = this.textInputControl.getLength();
    }

    void enterKeyPressed() {
      synchronized (this) {
        try {
          // �뻼耶쀣굮�뀯�뒟�걮�걼孃뚣겎�곣넀�굮�듉�걮�겍�궘�깵�꺃�긿�깉�굮�댗�걮�걼�걗�겏�겎Enter�굮�듉�걬�굦�걼�졃�릦�겗野양춺
          // Enter�굮�듉�걮�걼�셽�겘�곩섭�댍�쉪�겓�궘�깵�꺃�긿�깉�굮�쑌弱얇겓燁삣땿�걲�굥��
          this.textInputControl.positionCaret(this.textInputControl.getLength());

          final String lastLine = getLastLine();
          // �궎�깧�꺍�깉�겗�쇇�뵟�겘�곥깋�궘�깷�깳�꺍�깉�겗鸚됪쎍�굠�굤�굚�뀍�겎�걗�굥�걼�굙�곥걪�겗�셽�궧�겎�겘�뵻烏뚧뻼耶쀥닓�걣��永귟죱�겓�맜�겲�굦�겒�걚
          // 孃볝겂�겍�곫뵻烏뚣굮瓦썲뒥�걲�굥��
          final ByteBuffer buf = getCharset().encode(lastLine + "\r\n"); //$NON-NLS-1$
          this.inputTextTarget.write(buf.array(), 0, buf.remaining());
          this.inputTextTarget.flush();
          this.lastLineBreakIndex = this.textInputControl.getLength() + 1;
        } catch (IOException e) {
          if ("Read end dead".equals(e.getMessage())) {
            return;
          }
          throw new RuntimeException(e);
        }
      }
    }

    /**
     * ��永귟죱�뻼耶쀥닓�굮�룚孃쀣걮�겲�걲��
     *
     * @return ��永귟죱�뻼耶쀥닓
     */
    private String getLastLine() {
      synchronized (this) {
        return this.textInputControl.getText(this.lastLineBreakIndex, this.textInputControl.getLength());
      }
    }

    @Override
    public int available() throws IOException {
      return this.outputTextSource.available();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int read() throws IOException {
      try {
        return this.outputTextSource.read();
      } catch (IOException ex) {
        return -1;
      }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      try {
        return this.outputTextSource.read(b, off, len);
      } catch (IOException ex) {
        return -1;
      }
    }

    @Override
    public int read(final byte[] b) throws IOException {
      try {
        return this.outputTextSource.read(b);
      } catch (IOException ex) {
        return -1;
      }
    }

    @Override
    public void close() throws IOException {
      super.close();
    }

    void clear() throws IOException {
      this.inputTextTarget.flush();
      this.lastLineBreakIndex = 0;
    }
  }

  /**
   * �깇�궘�궧�깉�궠�꺍�깮�꺖�깓�꺍�깉�겓野얇걮�쎑�걤�걽�걲{@link OutputStream}若잒즳�겎�걲��
   *
   * @author Yuhi Ishikura
   */
  final class TextInputControlOutputStream extends OutputStream {

    private final TextInputControl textInputControl;
    private final CharsetDecoder decoder;
    private ByteArrayOutputStream buf;

    /**
     * {@link TextInputControlOutputStream}�궕�깣�궦�궒�궚�깉�굮礪뗧칹�걮�겲�걲��
     *
     * @param textInputControl �뻼耶쀣겗�눣�뒟�뀍
     */
    TextInputControlOutputStream(TextInputControl textInputControl) {
      this.textInputControl = textInputControl;
      this.decoder = getCharset().newDecoder();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void write(int b) throws IOException {
      synchronized (this) {
        if (this.buf == null) {
          this.buf = new ByteArrayOutputStream();
        }
        this.buf.write(b);
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void flush() throws IOException {
      Platform.runLater(() -> {
        try {
          flushImpl();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
    }

    private void flushImpl() throws IOException {
      synchronized (this) {
        if (this.buf == null) {
          return;
        }
        startProgramInput();
        final ByteBuffer byteBuffer = ByteBuffer.wrap(this.buf.toByteArray());
        final CharBuffer charBuffer = this.decoder.decode(byteBuffer);
        try {
          this.textInputControl.appendText(charBuffer.toString());
          this.textInputControl.positionCaret(this.textInputControl.getLength());
        } finally {
          this.buf = null;
          endProgramInput();
        }
      }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() throws IOException {
      flush();
    }

    void clear() throws IOException {
      this.buf = null;
    }

  }

}
