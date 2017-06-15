package com.alibaba.dubbo.remoting.exchange;

import java.util.Set;

/**
 * Created by haoning1 on 2017/6/13.
 */
public interface Cookie {
    /**
     * Returns the name of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @return The name of this {@link io.netty.handler.codec.http.Cookie}
     */
    String getName();

    /**
     * Returns the value of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @return The value of this {@link io.netty.handler.codec.http.Cookie}
     */
    String getValue();

    /**
     * Sets the value of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @param value The value to set
     */
    void setValue(String value);

    /**
     * Returns the domain of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @return The domain of this {@link io.netty.handler.codec.http.Cookie}
     */
    String getDomain();

    /**
     * Sets the domain of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @param domain The domain to use
     */
    void setDomain(String domain);

    /**
     * Returns the path of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @return The {@link io.netty.handler.codec.http.Cookie}'s path
     */
    String getPath();

    /**
     * Sets the path of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @param path The path to use for this {@link io.netty.handler.codec.http.Cookie}
     */
    void setPath(String path);

    /**
     * Returns the comment of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @return The comment of this {@link io.netty.handler.codec.http.Cookie}
     */
    String getComment();

    /**
     * Sets the comment of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @param comment The comment to use
     */
    void setComment(String comment);

    /**
     * Returns the maximum age of this {@link io.netty.handler.codec.http.Cookie} in seconds or {@link Long#MIN_VALUE} if unspecified
     *
     * @return The maximum age of this {@link io.netty.handler.codec.http.Cookie}
     */
    long getMaxAge();

    /**
     * Sets the maximum age of this {@link io.netty.handler.codec.http.Cookie} in seconds.
     * If an age of {@code 0} is specified, this {@link io.netty.handler.codec.http.Cookie} will be
     * automatically removed by browser because it will expire immediately.
     * If {@link Long#MIN_VALUE} is specified, this {@link io.netty.handler.codec.http.Cookie} will be removed when the
     * browser is closed.
     *
     * @param maxAge The maximum age of this {@link io.netty.handler.codec.http.Cookie} in seconds
     */
    void setMaxAge(long maxAge);

    /**
     * Returns the version of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @return The version of this {@link io.netty.handler.codec.http.Cookie}
     */
    int getVersion();

    /**
     * Sets the version of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @param version The new version to use
     */
    void setVersion(int version);

    /**
     * Checks to see if this {@link io.netty.handler.codec.http.Cookie} is secure
     *
     * @return True if this {@link io.netty.handler.codec.http.Cookie} is secure, otherwise false
     */
    boolean isSecure();

    /**
     * Sets the security getStatus of this {@link io.netty.handler.codec.http.Cookie}
     *
     * @param secure True if this {@link io.netty.handler.codec.http.Cookie} is to be secure, otherwise false
     */
    void setSecure(boolean secure);

    /**
     * Checks to see if this {@link io.netty.handler.codec.http.Cookie} can only be accessed via HTTP.
     * If this returns true, the {@link io.netty.handler.codec.http.Cookie} cannot be accessed through
     * client side script - But only if the browser supports it.
     * For more information, please look <a href="http://www.owasp.org/index.php/HTTPOnly">here</a>
     *
     * @return True if this {@link io.netty.handler.codec.http.Cookie} is HTTP-only or false if it isn't
     */
    boolean isHttpOnly();

    /**
     * Determines if this {@link io.netty.handler.codec.http.Cookie} is HTTP only.
     * If set to true, this {@link io.netty.handler.codec.http.Cookie} cannot be accessed by a client
     * side script. However, this works only if the browser supports it.
     * For for information, please look
     * <a href="http://www.owasp.org/index.php/HTTPOnly">here</a>.
     *
     * @param httpOnly True if the {@link io.netty.handler.codec.http.Cookie} is HTTP only, otherwise false.
     */
    void setHttpOnly(boolean httpOnly);

    /**
     * Returns the comment URL of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @return The comment URL of this {@link io.netty.handler.codec.http.Cookie}
     */
    String getCommentUrl();

    /**
     * Sets the comment URL of this {@link io.netty.handler.codec.http.Cookie}.
     *
     * @param commentUrl The comment URL to use
     */
    void setCommentUrl(String commentUrl);

    /**
     * Checks to see if this {@link io.netty.handler.codec.http.Cookie} is to be discarded by the browser
     * at the end of the current session.
     *
     * @return True if this {@link io.netty.handler.codec.http.Cookie} is to be discarded, otherwise false
     */
    boolean isDiscard();

    /**
     * Sets the discard flag of this {@link io.netty.handler.codec.http.Cookie}.
     * If set to true, this {@link io.netty.handler.codec.http.Cookie} will be discarded by the browser
     * at the end of the current session
     *
     * @param discard True if the {@link io.netty.handler.codec.http.Cookie} is to be discarded
     */
    void setDiscard(boolean discard);

    /**
     * Returns the ports that this {@link io.netty.handler.codec.http.Cookie} can be accessed on.
     *
     * @return The {@link Set} of ports that this {@link io.netty.handler.codec.http.Cookie} can use
     */
    Set<Integer> getPorts();

    /**
     * Sets the ports that this {@link io.netty.handler.codec.http.Cookie} can be accessed on.
     *
     * @param ports The ports that this {@link io.netty.handler.codec.http.Cookie} can be accessed on
     */
    void setPorts(int... ports);

    /**
     * Sets the ports that this {@link io.netty.handler.codec.http.Cookie} can be accessed on.
     *
     * @param ports The {@link Iterable} collection of ports that this
     *              {@link io.netty.handler.codec.http.Cookie} can be accessed on.
     */
    void setPorts(Iterable<Integer> ports);
}
