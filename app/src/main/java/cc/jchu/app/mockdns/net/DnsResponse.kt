package cc.jchu.app.mockdns.net

enum class DnsResponse(val code: Int, val description: String) {
    NOERROR(0, "DNS Query completed successfully"),
    FORMERR(1, "DNS Query Format Error"),
    SERVFAIL(2, "Server failed to complete the DNS request"),
    NXDOMAIN(3, "Domain name does not exist."),
    NOTIMP(4, "Function not implemented"),
    REFUSED(5, "The server refused to answer for the query"),
    YXDOMAIN(6, "Name that should not exist, does exist"),
    XRRSET(7, "Reset that should not exist, does exist"),
    NOTAUTH(8, "Server not authoritative for the zone"),
    NOTZONE(9, "Name not in zone"),
    UNKNOWN(-1, "Unknown response code")
}
