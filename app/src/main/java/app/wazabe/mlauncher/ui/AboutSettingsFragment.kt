package app.wazabe.mlauncher.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import app.wazabe.mlauncher.R
import com.google.android.material.card.MaterialCardView

class AboutSettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_about, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Set app version
        val versionTextView = view.findViewById<TextView>(R.id.appVersion)
        try {
            val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
            versionTextView.text = "Version ${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            versionTextView.text = "Version unknown"
        }
        
        // Setup GitHub card click
        val githubCard = view.findViewById<MaterialCardView>(R.id.githubCard)
        githubCard.setOnClickListener {
            openUrl("https://github.com/ChristopheVersieux/mLauncher")
        }
        
        // Setup Donation card click
        val donationCard = view.findViewById<MaterialCardView>(R.id.donationCard)
        donationCard.setOnClickListener {
            openUrl("https://www.paypal.com/donate/?business=Y4ZVAYMZCCD7A&no_recurring=0&currency_code=EUR")
        }
    }

    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            // Handle error silently
        }
    }
}
