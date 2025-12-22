package app.wazabe.mlauncher.ui.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.ui.onboarding.OnboardingPageFragment

class OnboardingAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    // Return the number of pages
    override fun getItemCount(): Int = 1  // Total number of pages in the onboarding flow

    // Return the corresponding fragment for each page
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OnboardingPageFragment.Companion.newInstance(R.layout.fragment_onboarding_page_one)
           // 1 -> OnboardingPageFragment.Companion.newInstance(R.layout.fragment_onboarding_page_two)
            //2 -> OnboardingPageFragment.Companion.newInstance(R.layout.fragment_onboarding_page_three)
            //3 -> OnboardingPageFragment.Companion.newInstance(R.layout.fragment_onboarding_page_four)
            else -> throw IllegalArgumentException("Invalid page position: $position")
        }
    }
}