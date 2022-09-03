package io.github.jwharm.javagi.interop;

import jdk.incubator.foreign.MemoryAddress;

public class ResourceBase implements NativeAddress {

    private Proxy proxy;

    public ResourceBase(Proxy proxy) {
        this.proxy = proxy;
        System.out.println(getClass().getSimpleName() + " -> " + proxy.HANDLE());
    }

    public MemoryAddress HANDLE() {
        return proxy.HANDLE();
    }
}
