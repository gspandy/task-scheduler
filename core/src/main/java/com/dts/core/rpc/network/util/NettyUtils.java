package com.dts.core.rpc.network.util;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.lang.reflect.Field;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.ServerChannel;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.internal.PlatformDependent;

/**
 * Created by zhangxin on 2016/11/26.
 */
public class NettyUtils {

  public static String getRemoteAddress(Channel channel) {
    if (channel != null && channel.remoteAddress() != null) {
      return channel.remoteAddress().toString();
    }
    return "unknown remote";
  }

  public static PooledByteBufAllocator createPooledByteBufAllocator(boolean allowDirectBufs,
      boolean allowCache, int numCores) {
    if (numCores == 0) {
      numCores = Runtime.getRuntime().availableProcessors();
    }
    return new PooledByteBufAllocator(
      allowDirectBufs && PlatformDependent.directBufferPreferred(),
      Math.min(getPrivateStaticField("DEFAULT_NUM_HEAP_ARENA"), numCores),
      Math.min(getPrivateStaticField("DEFAULT_NUM_DIRECT_ARENA"), allowDirectBufs ? numCores : 0),
      getPrivateStaticField("DEFAULT_PAGE_SIZE"),
      getPrivateStaticField("DEFAULT_MAX_ORDER"),
      allowCache ? getPrivateStaticField("DEFAULT_TINY_CACHE_SIZE") : 0,
      allowCache ? getPrivateStaticField("DEFAULT_SMALL_CACHE_SIZE") : 0,
      allowCache ? getPrivateStaticField("DEFAULT_NORMAL_CACHE_SIZE") : 0
      );
  }

  public static EventLoopGroup createEventLoop(IOMode mode, int numThreads, String threadPrefix) {
    ThreadFactory threadFactory = createThreadFactory(threadPrefix);
    switch (mode) {
      case NIO:
        return new NioEventLoopGroup(numThreads, threadFactory);
      case EPOLL:
        return new EpollEventLoopGroup(numThreads, threadFactory);
      default:
        throw new IllegalArgumentException("Unknown to mode: " + mode);
    }
  }

  public static ThreadFactory createThreadFactory(String threadPrefix) {
    return new ThreadFactoryBuilder().setDaemon(true).setNameFormat(threadPrefix + "-%d").build();
  }

  public static ThreadPoolExecutor newDaemonCachedThreadPool(String prefix, int maxThreadNum, int keepAliveSec) {
    ThreadFactory threadFactory = createThreadFactory(prefix);
    ThreadPoolExecutor threadPool = new ThreadPoolExecutor(
      maxThreadNum,
      maxThreadNum,
      keepAliveSec,
      TimeUnit.SECONDS,
      new LinkedBlockingQueue<>(),
      threadFactory
    );
    threadPool.allowCoreThreadTimeOut(true);
    return threadPool;
  }

  public static Class<? extends ServerChannel> getServerChannel(IOMode mode) {
    switch (mode) {
      case NIO:
        return NioServerSocketChannel.class;
      case EPOLL:
        return EpollServerSocketChannel.class;
      default:
        throw new IllegalArgumentException("Unknown io mode: " + mode);
    }
  }

  public static TransportFrameDecoder createFrameDecoder() {
    return new TransportFrameDecoder();
  }

  public static Class<? extends Channel> getClientChannelClass(IOMode mode) {
    switch (mode) {
      case NIO:
        return NioSocketChannel.class;
      case EPOLL:
        return EpollSocketChannel.class;
      default:
        throw new IllegalArgumentException("Unknown io mode: " + mode);
    }
  }

  private static int getPrivateStaticField(String name) {
    try {
      Field f = PooledByteBufAllocator.DEFAULT.getClass().getDeclaredField(name);
      f.setAccessible(true);
      return f.getInt(null);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
