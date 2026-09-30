package com.koodoagent.retrieval;


import java.net.URI;

/**
 * 入参 url字符串
 *     ↓
 * 1.判空：null/空白串 →抛异常
 *     ↓
 * 2.URI.create()解析字符串为URI对象，校验URL语法格式，格式非法抛异常
 *     ↓
 * 3.协议校验：只允许 http / https，其他协议直接拦截
 *     ↓
 * 4.host校验：host不能为null/空
 *     ↓
 * 5.域名黑名单：拦截 localhost / *.localhost / *.local / *.internal 内网域名
 *     ↓
 * 6.判断host是不是IP字面量
 *     ├─✅是IP字面量：isUnsafeIp()校验IP是否内网/回环。不安全抛异常；安全直接return，结束全部校验，跳过DNS解析
 *     └─❌是普通域名：往下执行DNS解析
 *         ↓
 * 7.InetAddress.getAllByName(host) DNS解析，拿到该域名全部IPV4/IPV6地址
 *     ├ DNS解析失败 →抛DNS resolve failed异常
 *     ↓
 * 8.循环遍历域名解析出来**每一个IP**，调用isUnsafeAddress()检测
 *     └只要任意一个IP属于危险地址，直接抛出异常：域名解析到内网IP
 *     ↓
 * 全部校验通过，方法正常返回，可以安全发起http请求
 */
import com.koodoagent.exception.AgentException;
import org.springframework.stereotype.Service;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Set;

@Service
public class UrlSafetyChecker {


    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    /** ALLOWED_SCHEMES：校验协议只能是 http/https，禁止 file 等危险协议
     * 检查 URL 是否安全。不安全直接抛 A004。
     * 通过 = 返回，不通过 = 抛异常。
     */
    public void check(String url) {
        if (url == null || url.isBlank()) {
            throw new AgentException("A004", "URL is empty");
        }

        URI uri;
        try {
            uri = URI.create(url);//把字符串 url 解析成 Java 的 URI 对象。
        } catch (Exception e) {
            throw new AgentException("A004", "Invalid URL: " + url);
        }

        // 1. 协议白名单
        String scheme = uri.getScheme();
        //uri.getScheme()
        //从 URI 对象拿到协议部分，例如：
        //https://baidu.com → 返回 "https"
        //http://xxx → 返回 "http"
        //没有协议的链接（baidu.com）→ 返回 null
        if (scheme == null || !ALLOWED_SCHEMES.contains(scheme.toLowerCase())) {
            throw new AgentException("A004", "Scheme not allowed: " + scheme);
        }

        // 2. host 不能为空
        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new AgentException("A004", "Host is empty: " + url);
        }

        String lowerHost = host.toLowerCase(); //把域名全部转小写，防止大写绕过，比如 LocalHost、TEST.LOCAL

        // 3. 明显本机/内网域名
        if (lowerHost.equals("localhost")
                || lowerHost.endsWith(".localhost")
                || lowerHost.endsWith(".local")
                || lowerHost.endsWith(".internal")) {
            throw new AgentException("A004", "Host not allowed: " + host);
        }

        // 4. 如果 host 本身就是 IP 字面量，直接检查
        if (isIpLiteral(lowerHost)) {    //判断这个 host 是不是纯 IP 地址字面量。
            if (isUnsafeIp(lowerHost)) {    //调用 isUnsafeIp() 判断是不是危险 IP：
                throw new AgentException("A007", "IP not allowed: " + host);
            }
            return; // IP 字面量不需要再做 DNS 解析
        }

        // 5. DNS 解析，检查所有 A/AAAA 记录
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);     //做 DNS 域名解析，传入域名字符串，向 DNS 服务器查询，返回这个域名绑定的全部 IP 地址数组
        } catch (UnknownHostException e) {
            throw new AgentException("A004", "DNS resolve failed: " + host);
        }

        for (InetAddress addr : addresses) {
            if (isUnsafeAddress(addr)) {
                throw new AgentException("A004",
                        "Host resolves to internal IP: " + host + " -> " + addr.getHostAddress());
            }
        }
    }

    /**
     * 判断 host 是不是 IP 字面量（IPv4 / IPv6）。
     */
    private boolean isIpLiteral(String host) {
        if (host.contains(":")) {
            return true; // IPv6
        }
        // IPv4: 只允许数字和点，且至少三个点
        if (host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
            return true;
        }
        return false;
    }

    private boolean isUnsafeIp(String ip) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            return isUnsafeAddress(addr);
        } catch (UnknownHostException e) {
            return true; // 解析失败当不安全处理
        }
    }

    private boolean isUnsafeAddress(InetAddress addr) {
        // 回环：127.0.0.1 / ::1
        if (addr.isLoopbackAddress()) return true;
        // 任意本地：0.0.0.0 / ::
        if (addr.isAnyLocalAddress()) return true;
        // 链路本地：169.254.x.x / fe80::
        if (addr.isLinkLocalAddress()) return true;
        // 站点本地：10.x / 172.16-31.x / 192.168.x
        if (addr.isSiteLocalAddress()) return true;
        // 组播
        if (addr.isMulticastAddress()) return true;

        // 额外的 IPv4 特殊段（isSiteLocalAddress 不覆盖的）
        if (addr instanceof Inet4Address) {
            byte[] b = addr.getAddress();
            int first = b[0] & 0xFF;
            int second = b[1] & 0xFF;
            int third = b[2] & 0xFF;

            // 100.64.0.0/10 CGNAT
            if (first == 100 && second >= 64 && second <= 127) return true;

            // 192.0.0.0/24 IETF protocol
            if (first == 192 && second == 0 && third == 0) return true;

            // 198.18.0.0/15 benchmark（⚠️ 见下方说明，暂时不拦）
            // 说明：Clash 的 fake-IP 模式会返回 198.18.x.x，
            // 如果拦掉，所有域名都会失败。
            // 198.18.0.0/15 benchmark
            //if (first == 198 && (second == 18 || second == 19)) return true;
        }

        // 云元数据地址 169.254.169.254 已被 isLinkLocalAddress 覆盖
        return false;
    }
}