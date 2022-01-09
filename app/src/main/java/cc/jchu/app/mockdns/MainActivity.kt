package cc.jchu.app.mockdns

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.DnsResolver
import android.net.VpnService
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import cc.jchu.app.mockdns.databinding.ActivityMainBinding
import cc.jchu.app.mockdns.net.extensions.DnsResponseExtension.mapResponse
import java.net.InetAddress
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val testVpnLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            ::handleTestVpnResult
        )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews(binding)
    }

    private fun initViews(binding: ActivityMainBinding) {
        binding.mockVpnDomainBtn.setOnClickListener {
            resolveNetwork()
        }

        binding.mockVpnBtnStart.setOnClickListener {
            testVpnServiceOrStart()
        }

        binding.mockVpnBtnStop.setOnClickListener {
            stopVpnService()
        }
    }

    private fun resolveNetwork() {
        val connMgr: ConnectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        val network = connMgr.activeNetwork ?: return
        val resolver = DnsResolver.getInstance()
        val domain = binding.mockVpnDomainInput.text?.toString() ?: ""

        resetResolvedInfo()

        if (domain.isNotEmpty()) {
            val callback = DnsResolveCallback(domain) {
                showResolvedInfo(it)
            }
            val flag: Int = DnsResolver.FLAG_NO_RETRY or
                DnsResolver.FLAG_NO_CACHE_STORE or
                DnsResolver.FLAG_NO_CACHE_LOOKUP
            resolver.query(network, domain, flag, mainExecutor, null, callback)
        } else {
            showResolvedInfo("No domain")
        }
    }

    private fun resetResolvedInfo() {
        val timestamp = DateFormat.getDateTimeInstance().format(Date())
        binding.mockVpnResult.text = "$timestamp\n"
    }

    private fun showResolvedInfo(message: String) {
        val text = binding.mockVpnResult.text ?: ""
        binding.mockVpnResult.text = "$text \n$message"
    }

    private fun testVpnServiceOrStart() {
        val intent = VpnService.prepare(this)
        if (intent != null) {
            testVpnLauncher.launch(intent)
        } else {
            startVpnService()
        }
    }

    private fun handleTestVpnResult(result: ActivityResult) {
        if (result.resultCode == RESULT_OK) {
            startVpnService()
        }
    }

    private fun startVpnService() {
        Log.d(TAG, "call Start service")
        val ip = binding.mockVpnDnsServerIpInput.text.toString()
        if (ip.isEmpty()) {
            showResolvedInfo("No DNS Server IP")
            return
        } else {
            MockVpnService.createNotificationChannel(this)
            MockVpnService.startMockService(this, ip)
        }
    }

    private fun stopVpnService() {
        Log.d(TAG, "call Stop service")
        MockVpnService.stopMockService(this)
    }

    private class DnsResolveCallback(
        private val domain: String,
        private val infoCallback: (String) -> Unit
    ) : DnsResolver.Callback<List<InetAddress>> {

        override fun onAnswer(answers: List<InetAddress>, responseCode: Int) {
            val sb = StringBuilder()
            sb.appendWithNewLine(responseCode.mapResponse().description)
            for (answer in answers) {
                sb.appendWithNewLine("resolved: $domain -> $answer")
            }
            Log.e(TAG, sb.toString())
            infoCallback(sb.toString())
        }

        override fun onError(error: DnsResolver.DnsException) {
            error.printStackTrace()
            infoCallback(error.toString())
            Log.e(TAG, error.toString())
        }
    }

    companion object {

        private const val TAG = "MockVpn"

        private fun StringBuilder.appendWithNewLine(charSequence: CharSequence) {
            this.append(charSequence)
            this.append("\n")
        }
    }
}
