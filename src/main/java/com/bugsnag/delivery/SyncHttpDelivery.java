package com.bugsnag.delivery;

import com.bugsnag.serialization.SerializationException;
import com.bugsnag.serialization.Serializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.UnknownHostException;

public class SyncHttpDelivery implements HttpDelivery {
    private static final Logger logger = LoggerFactory.getLogger(SyncHttpDelivery.class);

    protected static final String DEFAULT_ENDPOINT = "https://notify.bugsnag.com";
    protected static final int DEFAULT_TIMEOUT = 5000;

    protected String endpoint = DEFAULT_ENDPOINT;
    protected int timeout = DEFAULT_TIMEOUT;
    protected Proxy proxy;

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public void deliver(Serializer serializer, Object object) {
        HttpsURLConnection connection = null;
        try {
            URL url = new URL(endpoint);
            if (proxy != null) {
                connection = (HttpsURLConnection) url.openConnection(proxy);
            } else {
                connection = (HttpsURLConnection) url.openConnection();
            }

            connection.setDoOutput(true);
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(timeout);
            connection.addRequestProperty("Content-Type", "application/json");

            OutputStream outputStream = null;
            try {
                outputStream = connection.getOutputStream();
                serializer.writeToStream(outputStream, object);
            } finally {
                try {
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (final IOException ioe) {
                    // Don't care
                }
            }

            // End the request, get the response code
            int status = connection.getResponseCode();
            if (status / 100 != 2) {
                logger.warn(
                        "Error not reported to Bugsnag - got non-200 response code: {}", status);
            }
        } catch (SerializationException ex) {
            logger.warn("Error not reported to Bugsnag - exception when serializing payload", ex);
        } catch (UnknownHostException ex) {
            logger.warn("Error not reported to Bugsnag - unknown host {}", endpoint);
        } catch (IOException ex) {
            logger.warn("Error not reported to Bugsnag - exception when making request", ex);
        } finally {
            connection.disconnect();
        }
    }

    @Override
    public void close() {
        // Nothing to do here.
    }
}
