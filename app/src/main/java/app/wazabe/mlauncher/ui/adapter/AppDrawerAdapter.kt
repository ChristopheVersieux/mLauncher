/**
 * Prepare the data for the app drawer, which is the list of all the installed applications.
 */

package app.wazabe.mlauncher.ui.adapter

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.core.graphics.ColorUtils
import android.graphics.PorterDuff
import android.os.UserHandle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.Filter
import android.widget.Filterable
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.view.Gravity
import androidx.appcompat.content.res.AppCompatResources
import androidx.biometric.BiometricPrompt
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.common.ColorIconsExtensions

import com.github.droidworksstudio.fuzzywuzzy.FuzzyFinder
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.AppListItem
import app.wazabe.mlauncher.data.Constants
import app.wazabe.mlauncher.data.Constants.AppDrawerFlag
import app.wazabe.mlauncher.data.Prefs

import app.wazabe.mlauncher.helper.IconCacheTarget
import app.wazabe.mlauncher.helper.IconPackHelper.getSafeAppIcon
import app.wazabe.mlauncher.helper.dp2px
import app.wazabe.mlauncher.helper.getSystemIcons
import app.wazabe.mlauncher.helper.utils.BiometricHelper
import app.wazabe.mlauncher.helper.utils.visibleHideLayouts
import com.github.droidworksstudio.common.isSystemApp
import com.github.droidworksstudio.common.showKeyboard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

class AppDrawerAdapter(
    private val context: Context,
    private val fragment: Fragment,
    internal var flag: AppDrawerFlag,
    private val gravity: Int,
    private val appClickListener: (AppListItem) -> Unit,
    private val appDeleteListener: (AppListItem) -> Unit,
    private val appRenameListener: (String, String) -> Unit,
    private val appTagListener: (String, String, UserHandle) -> Unit,
    private val appHideListener: (AppDrawerFlag, AppListItem) -> Unit,
    private val appInfoListener: (AppListItem) -> Unit
) : RecyclerView.Adapter<AppDrawerAdapter.ViewHolder>(), Filterable {

    private var prefs = Prefs(fragment.requireContext())
    private var appFilter = createAppFilter()
    var location: Int = 0
    var appsList: MutableList<AppListItem> = mutableListOf()
    var appFilteredList: MutableList<AppListItem> = mutableListOf()
    private lateinit var biometricHelper: BiometricHelper

    // Add icon cache
    private val iconCache = ConcurrentHashMap<String, Drawable?>()
    private val iconLoadingScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var isBangSearch = false

    override fun getItemViewType(position: Int): Int {
        return when (prefs.drawerAlignment) {
            Constants.Gravity.Left -> 0
            Constants.Gravity.Center -> 1
            Constants.Gravity.Right -> 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutRes = when (viewType) {
            0 -> R.layout.item_app_left
            1 -> R.layout.item_app_center
            2 -> R.layout.item_app_right
            else -> R.layout.item_app_left
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        
        biometricHelper = BiometricHelper(fragment.requireActivity())
        return ViewHolder(view)
    }

    fun getItemAt(position: Int): AppListItem? {
        return if (position in appsList.indices) appsList[position] else null
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (appFilteredList.isEmpty() || position !in appFilteredList.indices) {
            AppLogger.d("AppListDebug", "⚠️ onBindViewHolder called but appFilteredList is empty or position out of bounds")
            return
        }

        val appModel = appFilteredList[holder.absoluteAdapterPosition]
        AppLogger.d("AppListDebug", "🔧 Binding position=$position, label=${appModel.label}, package=${appModel.activityPackage}")
        holder.bind(flag, gravity, appModel, appClickListener, appInfoListener, appDeleteListener, iconCache, iconLoadingScope, prefs)

        autoLaunch(position)

        holder.itemView.setOnLongClickListener {
             val openApp = flag == AppDrawerFlag.LaunchApp || flag == AppDrawerFlag.HiddenApps
             if (!openApp) return@setOnLongClickListener true

             val popup = androidx.appcompat.widget.PopupMenu(holder.itemView.context, holder.appTitle, Gravity.START)
             val menu = popup.menu
             if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                 menu.setGroupDividerEnabled(true)
             }

             try {
                val field = popup.javaClass.getDeclaredField("mPopup")
                field.isAccessible = true
                val menuPopupHelper = field.get(popup)
                val classPopupHelper = Class.forName(menuPopupHelper.javaClass.name)
                val setForceIcons = classPopupHelper.getMethod("setForceShowIcon", Boolean::class.javaPrimitiveType)
                setForceIcons.invoke(menuPopupHelper, true)
             } catch (e: Exception) { e.printStackTrace() }

             val ctx = holder.itemView.context
             
             fun getThemedIcon(resId: Int): Drawable? {
                 return AppCompatResources.getDrawable(ctx, resId)
             }

             val pkg = appModel.activityPackage
             val isLocked = prefs.lockedApps.contains(pkg)
             val isPinned = prefs.pinnedApps.contains(pkg)
             val isHidden = (flag == AppDrawerFlag.HiddenApps)
             val contextMenuFlags = prefs.getMenuFlags("CONTEXT_MENU_FLAGS", "0011111")

             if (contextMenuFlags[1]) {
                  val title = if (isLocked) holder.itemView.context.getString(R.string.unlock) else holder.itemView.context.getString(R.string.lock)
                  val icon = if (isLocked) R.drawable.padlock_off else R.drawable.padlock
                  menu.add(0, 1, 0, title).icon = getThemedIcon(icon)
             }
             if (contextMenuFlags[0]) {
                  val title = if (isPinned) holder.itemView.context.getString(R.string.unpin) else holder.itemView.context.getString(R.string.pin)
                  val icon = if (isPinned) R.drawable.pin_off else R.drawable.pin
                  menu.add(0, 2, 1, title).icon = getThemedIcon(icon)
             }
             if (contextMenuFlags[2]) {
                  val title = if (isHidden) holder.itemView.context.getString(R.string.show) else holder.itemView.context.getString(R.string.hide)
                  val icon = if (isHidden) R.drawable.visibility else R.drawable.visibility_off
                  menu.add(0, 3, 2, title).icon = getThemedIcon(icon)
             }

             if (contextMenuFlags[3]) {
                  menu.add(1, 4, 3, holder.itemView.context.getString(R.string.rename)).icon = getThemedIcon(R.drawable.ic_rename)
             }
             if (contextMenuFlags[4]) {
                  menu.add(1, 5, 4, holder.itemView.context.getString(R.string.tag)).icon = getThemedIcon(R.drawable.ic_tag)
             }
             menu.add(1, 6, 5, "App Info").icon = getThemedIcon(R.drawable.ic_info)
             
             menu.add(2, 7, 6, "Uninstall").icon = getThemedIcon(R.drawable.ic_delete)

             popup.setOnMenuItemClickListener { item ->
                 when(item.itemId) {
                     1 -> { // Lock/Unlock
                         val currentLockedApps = prefs.lockedApps.toMutableSet()
                         if (isLocked) {
                             biometricHelper.startBiometricAuth(appModel, object : BiometricHelper.CallbackApp {
                                 override fun onAuthenticationSucceeded(appListItem: AppListItem) {
                                     currentLockedApps.remove(pkg)
                                     prefs.lockedApps = currentLockedApps
                                     notifyItemChanged(holder.absoluteAdapterPosition)
                                 }
                                 override fun onAuthenticationFailed() {}
                                 override fun onAuthenticationError(errorCode: Int, errorMessage: CharSequence?) {}
                             })
                         } else {
                             currentLockedApps.add(pkg)
                             prefs.lockedApps = currentLockedApps
                             notifyItemChanged(holder.absoluteAdapterPosition)
                         }
                     }
                     2 -> { // Pin/Unpin
                         val currentPinnedApps = prefs.pinnedApps.toMutableSet()
                         if (isPinned) currentPinnedApps.remove(pkg) else currentPinnedApps.add(pkg)
                         prefs.pinnedApps = currentPinnedApps
                         notifyItemChanged(holder.absoluteAdapterPosition)
                     }
                     3 -> { // Hide/Show
                         appFilteredList.removeAt(holder.absoluteAdapterPosition)
                         appsList.remove(appModel)
                         notifyItemRemoved(holder.absoluteAdapterPosition)
                         appHideListener(flag, appModel)
                     }
                     4 -> { // Rename
                         appRenameListener(appModel.activityPackage, appModel.customLabel)
                     }
                     5 -> { // Tag
                         appTagListener(appModel.activityPackage, appModel.customTag, appModel.user)
                     }
                     6 -> appInfoListener(appModel)
                     7 -> appDeleteListener(appModel)
                 }
                 true
             }
             popup.show()
             true
        }
    }

    override fun getItemCount(): Int = appFilteredList.size

    override fun getFilter(): Filter = this.appFilter

    private fun createAppFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSearch: CharSequence?): FilterResults {
                isBangSearch = listOf("#").any { prefix -> charSearch?.startsWith(prefix) == true }
                prefs = Prefs(context)

                val searchChars = charSearch.toString().trim().lowercase()
                val filteredApps: MutableList<AppListItem>

                val isTagSearch = searchChars.startsWith("#")
                val query = if (isTagSearch) searchChars.substringAfter("#") else searchChars
                val normalizeField: (AppListItem) -> String = { app -> if (isTagSearch) normalize(app.tag) else normalize(app.label) }

                // Scoring logic
                val scoredApps: Map<AppListItem, Int> = if (prefs.enableFilterStrength) {
                    appsList.associateWith { app ->
                        if (isTagSearch) {
                            FuzzyFinder.scoreString(normalize(app.tag), query, Constants.MAX_FILTER_STRENGTH)
                        } else {
                            FuzzyFinder.scoreApp(app, query, Constants.MAX_FILTER_STRENGTH)
                        }
                    }
                } else {
                    emptyMap()
                }

                filteredApps = if (searchChars.isEmpty()) {
                    appsList.toMutableList()
                } else {
                    val filtered = if (prefs.enableFilterStrength) {
                        // Filter using scores
                        scoredApps.filter { (app, score) ->
                            (prefs.searchFromStart && normalizeField(app).startsWith(query) ||
                                    !prefs.searchFromStart && normalizeField(app).contains(query))
                                    && score > prefs.filterStrength
                        }.map { it.key }
                    } else {
                        // Filter without scores
                        appsList.filter { app ->
                            if (prefs.searchFromStart) {
                                normalizeField(app).startsWith(query)
                            } else {
                                FuzzyFinder.isMatch(normalizeField(app), query)
                            }
                        }
                    }

                    filtered.toMutableList()
                }

                if (query.isNotEmpty()) AppLogger.d("searchQuery", query)

                val filterResults = FilterResults()
                filterResults.values = filteredApps
                return filterResults
            }

            fun normalize(input: String): String {
                // Normalize to NFC to keep composed characters (é stays é, not e + ´)
                val temp = Normalizer.normalize(input, Normalizer.Form.NFC)
                return temp
                    .lowercase()                  // lowercase Latin letters; other scripts unaffected
                    .filter { it.isLetterOrDigit() } // keep letters/digits from any language, including accented letters
            }


            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results?.values is MutableList<*>) {
                    appFilteredList = results.values as MutableList<AppListItem>
                    notifyDataSetChanged()
                } else {
                    return
                }
            }
        }
    }

    private fun autoLaunch(position: Int) {
        val lastMatch = itemCount == 1
        val openApp = flag == AppDrawerFlag.LaunchApp
        val autoOpenApp = prefs.autoOpenApp
        if (lastMatch && openApp && autoOpenApp) {
            try { // Automatically open the app when there's only one search result
                if (isBangSearch.not()) appClickListener(appFilteredList[position])
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setAppList(appsList: MutableList<AppListItem>) {
        this.appsList = appsList
        this.appFilteredList = appsList
        notifyDataSetChanged()
    }

    fun launchFirstInList() {
        if (appFilteredList.isNotEmpty())
            appClickListener(appFilteredList[0])
    }

    fun getFirstInList(): String? {
        if (appFilteredList.isNotEmpty())
            return appFilteredList[0].label
        return null
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        // Optionally clear icon to avoid wrong icons on recycled views
        holder.clearIcon()
    }

    class ViewHolder(
        view: View
    ) : RecyclerView.ViewHolder(view) {
        val appTitle: TextView = view.findViewById(R.id.appTitle)
        val appIcon: android.widget.ImageView = view.findViewById(R.id.appIcon)

        @SuppressLint("RtlHardcoded", "NewApi")
        fun bind(
            flag: AppDrawerFlag,
            appLabelGravity: Int,
            appListItem: AppListItem,
            appClickListener: (AppListItem) -> Unit,
            appInfoListener: (AppListItem) -> Unit,
            appDeleteListener: (AppListItem) -> Unit,
            iconCache: ConcurrentHashMap<String, Drawable?>,
            iconLoadingScope: CoroutineScope,
            prefs: Prefs
        ) {
            val context = itemView.context
            val packageName = appListItem.activityPackage

            // Set app title
            appTitle.text = appListItem.label
            
            // Set text color
            // Text Color is handled by system theme (?android:attr/textColorPrimary)
            appTitle.textSize = prefs.appSize.toFloat()

            if (app.wazabe.mlauncher.Mlauncher.prefs.launcherFont != "system") {
                appTitle.typeface = app.wazabe.mlauncher.Mlauncher.globalTypeface
            }

            // Icon loading
            val placeholderIcon = AppCompatResources.getDrawable(context, R.drawable.ic_default_app)
            val cachedIcon = iconCache[packageName]
            setAppTitleIcon(appTitle, cachedIcon ?: placeholderIcon, prefs)

            if (cachedIcon == null && packageName.isNotBlank() && prefs.iconPackAppList != Constants.IconPacks.Disabled) {
                iconLoadingScope.launch {
                    val icon = withContext(Dispatchers.IO) {
                        val nonNullDrawable: Drawable = getSafeAppIcon(
                            context = context,
                            packageName = packageName,
                            useIconPack = prefs.customIconPackAppList.isNotEmpty() &&
                                    prefs.iconPackAppList == Constants.IconPacks.Custom,
                            iconPackTarget = IconCacheTarget.APP_LIST
                        )
                        getSystemIcons(context, prefs, IconCacheTarget.APP_LIST, nonNullDrawable) ?: nonNullDrawable
                    }
                    iconCache[packageName] = icon
                    if (appTitle.text == appListItem.label) setAppTitleIcon(appTitle, icon, prefs)
                }
            }

            // Click listener on entire item
            itemView.setOnClickListener { appClickListener(appListItem) }
        }


        // Helper to set icon on appIcon ImageView
        private fun setAppTitleIcon(appTitle: TextView, icon: Drawable?, prefs: Prefs) {
            if (icon == null || prefs.iconPackAppList == Constants.IconPacks.Disabled) {
                appIcon.visibility = android.view.View.GONE
                return
            }
            appIcon.visibility = android.view.View.VISIBLE
            appIcon.setImageDrawable(icon)
        }

        // Clear icon when view is recycled
        fun clearIcon() {
            appIcon.setImageDrawable(null)
        }
    }
}
