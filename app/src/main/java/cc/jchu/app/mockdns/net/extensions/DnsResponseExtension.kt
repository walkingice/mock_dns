package cc.jchu.app.mockdns.net.extensions

import cc.jchu.app.mockdns.net.DnsResponse

object DnsResponseExtension {

    fun Int.mapResponse(): DnsResponse {
        for (type in DnsResponse.values()) {
            if (type.code == this) {
                return type
            }
        }
        return DnsResponse.UNKNOWN
    }
}
