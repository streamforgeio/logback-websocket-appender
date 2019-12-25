package net.unit8.logback;

import ch.qos.logback.classic.net.LoggingEventPreSerializationTransformer;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.encoder.Encoder;
import ch.qos.logback.core.util.CloseUtil;

import javax.websocket.*;
import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;

/**
 * @author kawasima
 */
public class WebSocketAppender extends AppenderBase<ILoggingEvent> {
    private URI serverUri;
    private Session session;

    /**
     * It is the encoder which is ultimately responsible for writing the event to
     * an {@link OutputStream}.
     */
    protected Encoder<ILoggingEvent> encoder;

    public Encoder<ILoggingEvent> getEncoder() {
        return encoder;
    }

    public void setEncoder(Encoder<ILoggingEvent> encoder) {
        this.encoder = encoder;
    }

    private void started() {
        super.start();
    }

    @Override
    public void start() {
        if (isStarted()) return;

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        try {
            session = container.connectToServer(new Endpoint() {
                @Override
                public void onOpen(Session session, EndpointConfig endpointConfig) {
                    started();
                }

                @Override
                public void onClose(Session session, CloseReason closeReason) {
                }
            }, ClientEndpointConfig.Builder.create().build(), serverUri);
        } catch (Exception ex) {
            addError("Connect server error", ex);
        }
    }

    @Override
    public void stop() {
        if (!isStarted()) return;
        CloseUtil.closeQuietly(session);
        super.stop();
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (event == null || !isStarted()) return;

        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        RemoteEndpoint.Async remote = session.getAsyncRemote();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.write(encoder.encode(event));
            oos.flush();
            remote.sendBinary(ByteBuffer.wrap(baos.toByteArray()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

    }

    public void setServerUri(String uri) {
        this.serverUri = URI.create(uri);
    }
}
