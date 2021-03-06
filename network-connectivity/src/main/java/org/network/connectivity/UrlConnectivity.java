package org.network.connectivity;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class UrlConnectivity implements ConnectivityCheck {

    private Queue<CheckServer> servers = new LinkedList<>();
    private Proxy proxy;

    public UrlConnectivity(Proxy proxy) {
        this.proxy = proxy;
        this.servers.addAll(Arrays.asList(CheckServer.values()));
    }

    public void setProxy(Proxy proxy) {
        this.proxy = proxy;
    }


    @Override
    public boolean isOnline() {
        return NetworkStatus.Connected.equals(networkStatus());
    }

    /**
     * Perform an HTTP GET request to check network connectivity
     * if we got any response (code = 200) connected <br>
     * if ISP redirect the request redirected <br>
     * if got any network / IO exception  disconnected <br>
     *
     * @return NetworkStatus Object
     */
    @Override
    public NetworkStatus networkStatus() {
        CheckServer server = servers.poll();
        try {
            URL url = new URL(server.url());
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection(proxy);
            connection.setInstanceFollowRedirects(false);
            connection.connect();
            return isRedirect(connection.getResponseCode()) ?
                    NetworkStatus.Redirected : NetworkStatus.Connected;
        } catch (IOException e) {
            return NetworkStatus.Disconnected;
        } finally {
            servers.offer(server);
        }
    }

    @Override
    public NetworkReport networkReport() {
        CheckServer server = servers.poll();
        try {
            URL url = new URL(server.url());
            HttpURLConnection connection =
                    (HttpURLConnection) url.openConnection(proxy);
            connection.setInstanceFollowRedirects(false);
            connection.connect();
            return new NetworkReport(
                    isRedirect(connection.getResponseCode()) ?
                            NetworkStatus.Redirected : NetworkStatus.Connected,
                    server.serverName(),
                    connection.getResponseCode() + " " + connection.getResponseMessage());
        } catch (IOException e) {
            return new NetworkReport(
                    NetworkStatus.Disconnected,
                    server.serverName(),
                    e.getMessage());
        } finally {
            servers.offer(server);
        }
    }

    private boolean isRedirect(int code) {
        switch (code) {
            case 300: // Multiple Choice
            case 301: // Moved Permanently
            case 302:    // Found
            case 303: // See Other
            case 307: // Temporary Redirect
            case 308: // Permanent Redirect
                return true;
            default:
                return false;
        }
    }


}
