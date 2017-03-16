package org.hcjf.io.net.http;

import org.hcjf.errors.Errors;
import org.hcjf.io.net.NetClient;
import org.hcjf.io.net.NetPackage;
import org.hcjf.io.net.NetService;
import org.hcjf.io.net.NetSession;
import org.hcjf.log.Log;
import org.hcjf.properties.SystemProperties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.UUID;

/**
 * @author javaito
 * @mail javaito@gmail.com
 */
public class HttpClient extends NetClient<HttpSession, HttpPackage> {

    public static final String HTTP_CLIENT_LOG_TAG = "HTTP_CLIENT";
    private static final String HTTP_PROTOCOL = "http";
    private static final String HTTPS_PROTOCOL = "https";

    private static final String CONNECTION_TIMEOUT_MESSAGE = "Connection timeout";
    private static final String READ_TIMEOUT_MESSAGE = "Read timeout";
    private static final String DISCONNECTION_MESSAGE = "Http client request end";
    private static final String SESSION_NAME = "Http client session";

    private final URL url;
    private Status status;
    private HttpResponse response;
    private HttpRequest request;
    private Long connectTimeout;
    private Long writeTimeout;
    private Long readTimeout;
    private HttpSession session;

    public HttpClient(URL url) {
        super(url.getHost(), url.getPort() != -1 ? url.getPort() :
                SystemProperties.getInteger(SystemProperties.Net.Http.DEFAULT_CLIENT_PORT),
                url.getProtocol().equals(HTTPS_PROTOCOL) ? NetService.TransportLayerProtocol.TCP_SSL : NetService.TransportLayerProtocol.TCP);
        this.url = url;
        this.connectTimeout = SystemProperties.getLong(SystemProperties.Net.Http.DEFAULT_CLIENT_CONNECT_TIMEOUT);
        this.writeTimeout = SystemProperties.getLong(SystemProperties.Net.Http.DEFAULT_CLIENT_WRITE_TIMEOUT);
        this.readTimeout = SystemProperties.getLong(SystemProperties.Net.Http.DEFAULT_CLIENT_READ_TIMEOUT);
        init();
    }

    private void init() {
        //Init defaults
        this.response = null;
        this.status = Status.INACTIVE;

        //Create default request
        request = new HttpRequest();
        request.setHttpVersion(HttpVersion.VERSION_1_1);
        request.setContext(url.getFile());
        request.setMethod(HttpMethod.GET);
        request.setBody(new byte[0]);
        request.addHeader(new HttpHeader(HttpHeader.HOST, url.getHost()));
        request.addHeader(new HttpHeader(HttpHeader.USER_AGENT, HttpHeader.DEFAULT_USER_AGENT));
    }

    /**
     * Return the connection timeout value.
     * @return Connection timeout vaue.
     */
    public final Long getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * Set the connection timeout value.
     * @param connectTimeout Connection timeout value.
     */
    public final void setConnectTimeout(Long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    /**
     * Return the write timeout value.
     * @return Write timeout value.
     */
    public final Long getWriteTimeout() {
        return writeTimeout;
    }

    /**
     * Set the write timeout value.
     * @param writeTimeout Write timeout value.
     */
    public final void setWriteTimeout(Long writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    /**
     * Return the read timeout value.
     * @return Read timeout value.
     */
    public final Long getReadTimeout() {
        return readTimeout;
    }

    /**
     * Set the read timeout value.
     * @param readTimeout Read timeout value.
     */
    public final void setReadTimeout(Long readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Add path over base context of the constructor URI.
     * @param paths Adding paths.
     */
    public void addPath(String... paths) {
        StringBuilder newPath = new StringBuilder();
        newPath.append(this.request.getContext());
        String separator = this.request.getContext().endsWith(
                HttpPackage.HTTP_CONTEXT_SEPARATOR) ? "" : HttpPackage.HTTP_CONTEXT_SEPARATOR;
        for(String path : paths) {
            newPath.append(separator).append(path);
            separator = HttpPackage.HTTP_CONTEXT_SEPARATOR;
        }
        this.request.setContext(newPath.toString());
    }

    /**
     * Return all the internal fields to the default values
     * in order to reuse this client.
     */
    public final void reset() {
        init();
    }

    /**
     * Set the request body.
     * @param body Request body.
     */
    public void setBody(byte[] body) {
        request.setBody(body);
    }

    /**
     * Set the http method to request.
     * @param method Http method.
     */
    public final void setHttpMethod(HttpMethod method) {
        request.setMethod(method);
    }

    /**
     * Add header to request.
     * @param header Http header.
     */
    public final void addHttpHeader(String header) {
        request.addHeader(new HttpHeader(header));
    }

    /**
     * Add header to request.
     * @param header Http header.
     */
    public final void addHttpHeader(HttpHeader header) {
        request.addHeader(header);
    }

    /**
     *
     * @return
     */
    @Override
    protected SSLEngine createSSLEngine() {
        try {
            SSLEngine engine = SSLContext.getDefault().createSSLEngine();
            engine.setUseClientMode(true);
            engine.beginHandshake();
            return engine;
        } catch (Exception ex) {
            throw new IllegalArgumentException(Errors.getMessage(Errors.ORG_HCJF_IO_NET_HTTP_6), ex);
        }
    }

    /**
     * This method return the object that represent the
     * client's session.
     * @return Client's session.
     */
    @Override
    public HttpSession getSession() {
        return session;
    }

    @Override
    public HttpSession checkSession(HttpSession session, HttpPackage payLoad, NetPackage netPackage) {
        return session;
    }

    /**
     * This method decode the implementation data.
     * @param payLoad Implementation data.
     * @return Implementation data encoded.
     */
    @Override
    protected byte[] encode(HttpPackage payLoad) {
        byte[] result = null;
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(payLoad.getProtocolHeader());
            if(payLoad.getBody() != null) {
                out.write(payLoad.getBody());
            }
            out.flush();
            result = out.toByteArray();
        } catch (Exception ex){}
        return result;
    }

    /**
     * This method decode the net package to obtain the implementation data
     * @param netPackage Net package.
     * @return Return the implementation data.
     */
    @Override
    protected HttpPackage decode(NetPackage netPackage) {
        if(response == null) {
            response = new HttpResponse();
        }
        response.addData(netPackage.getPayload());
        return response;
    }

    /**
     * Destroy the session.
     * @param session Net session to be destroyed
     */
    @Override
    public void destroySession(NetSession session) {
        session = null;
    }

    /**
     *
     * @param session Connected session.
     * @param payLoad Decoded package payload.
     * @param netPackage Original package.
     */
    @Override
    protected void onConnect(HttpSession session, HttpPackage payLoad, NetPackage netPackage) {
        synchronized (this) {
            status = Status.CONNECTED;
            notifyAll();
        }
    }

    /**
     *
     * @param session Net session.
     * @param payLoad Net package decoded
     * @param netPackage Net package.
     */
    @Override
    protected final void onRead(HttpSession session, HttpPackage payLoad, NetPackage netPackage) {
        if(response.isComplete()) {
            synchronized (this) {
                status = Status.DONE;
                notifyAll();
            }
        }
    }

    /**
     * This method execute all the steps to do a http request. Creates the connection,
     * sends the request package and reads the response, then this response
     * is returned as a method response.
     * @return Http response package.
     */
    public final HttpResponse request() {
        session = new HttpSession(UUID.randomUUID(), this);
        session.setRequest(request);
        session.setSessionName(SESSION_NAME);
        Integer errorCode = null;
        String errorPhrase = null;

        //Connection block
        status = Status.CONNECTING;
        synchronized (this) {
            connect();
            if (status == Status.CONNECTING) {
                try {
                    wait(getConnectTimeout());
                } catch (InterruptedException e) {}
            }

            if (status == Status.CONNECTING) {
                status = Status.ERROR;
                errorCode = HttpResponseCode.REQUEST_TIMEOUT;
                errorPhrase = CONNECTION_TIMEOUT_MESSAGE;
            }
        }

        //Request writing request and read response block
        if(status != Status.ERROR) {
            status = Status.WRITING;

            synchronized (this) {
                Log.out(HTTP_CLIENT_LOG_TAG, "Http client request\r\n%s", request.toString());
                try {
                    write(getSession(), request);
                } catch (IOException ex) {
                    status = Status.ERROR;
                    errorCode = HttpResponseCode.BAD_REQUEST;
                    errorPhrase = ex.getMessage();
                }

                if (status == Status.WRITING) {
                    try {
                        wait(getReadTimeout());
                    } catch (InterruptedException e) {
                    }
                }

                if (status == Status.WRITING) {
                    status = Status.ERROR;
                    errorCode = HttpResponseCode.REQUEST_TIMEOUT;
                    errorPhrase = READ_TIMEOUT_MESSAGE;
                }
            }
        }

        HttpResponse response;
        if(status == Status.ERROR) {
            response = new HttpResponse();
            response.setHttpVersion(HttpVersion.VERSION_1_1);
            response.setResponseCode(errorCode);
            response.setReasonPhrase(errorPhrase);
        } else {
            response = this.response;
        }

        disconnect(getSession(), DISCONNECTION_MESSAGE);

        Log.in(HTTP_CLIENT_LOG_TAG, "Http client response\r\n%s", response.toString());
        return response;
    }

    /**
     * Clietn request status.
     */
    private enum Status {

        INACTIVE,

        CONNECTING,

        CONNECTED,

        WRITING,

        DONE,

        ERROR;

    }
}
