package socks5.message;

import socks5.Proxy;

import java.util.Arrays;

public class ResponseOnRequestSocks {
    public static final byte COMMAND_NOT_SUPPORTED = 0x07;
    public static final byte ADDRESS_TYPE_NOT_SUPPORTED = 0x08;
    public static final byte SUCCEEDED = 0x00;
    public static final byte HOST_NOT_AVAILABLE = 0x04;

    public static byte[] create(RequestSocks request, boolean isConnected) {
        byte[] data = Arrays.copyOf(request.getBytes(), request.getBytes().length);
        data[0] = Proxy.SOCKS_5;
        data[1] = SUCCEEDED;
        if (!request.isCommand(RequestSocks.CONNECT_TCP)) {
            data[1] = COMMAND_NOT_SUPPORTED;
        }
        if (!isConnected) {
            data[1] = HOST_NOT_AVAILABLE;
        }
        if (request.getAddressType() == RequestSocks.IPv6) {
            data[1] = ADDRESS_TYPE_NOT_SUPPORTED;
        }
        return data;
    }
}