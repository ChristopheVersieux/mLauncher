package app.wazabe.mlauncher.ui

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import app.wazabe.mlauncher.MainViewModel
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.Constants.AppDrawerFlag
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.data.AppListItem
import app.wazabe.mlauncher.ui.adapter.AppDrawerAdapter
import app.wazabe.mlauncher.databinding.FragmentHiddenAppsListBinding

class HiddenAppsFragment : Fragment() {

    private val viewModel: MainViewModel by activityViewModels()
    private var _binding: FragmentHiddenAppsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: AppDrawerAdapter
    private lateinit var prefs: Prefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHiddenAppsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

        // Setup Adapter
        adapter = AppDrawerAdapter(
            context = requireContext(),
            fragment = this,
            flag = AppDrawerFlag.HiddenApps,
            gravity = Gravity.START,
            appClickListener = { app -> toggleAppVisibility(app) },
            appLongClickListener = { } // No long press needed
        )

        binding.appsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            this.adapter = this@HiddenAppsFragment.adapter
        }

        // Load and Observe
        viewModel.getHiddenApps()
        
        viewModel.hiddenApps.observe(viewLifecycleOwner) { apps ->
            val isEmpty = apps.isNullOrEmpty()
            binding.emptyStateContainer.isVisible = isEmpty
            binding.appsRecyclerView.isVisible = !isEmpty
            apps?.let {
                adapter.setAppList(it.toMutableList())
            }
        }
    }

    private fun toggleAppVisibility(app: AppListItem) {
        viewModel.toggleAppVisibility(app)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
