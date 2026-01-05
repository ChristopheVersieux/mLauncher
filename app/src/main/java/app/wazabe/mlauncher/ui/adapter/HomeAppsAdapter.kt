import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.Constants
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.helper.IconCacheTarget
import app.wazabe.mlauncher.helper.IconPackHelper

class HomeAppsAdapter(
    private val prefs: Prefs,
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<HomeAppsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.appTitle)
        val appIcon: ImageView = view.findViewById(R.id.appIcon)
    }

    override fun getItemViewType(position: Int): Int {
        return when (prefs.homeAlignment) {
            Constants.Gravity.Left -> 0
            Constants.Gravity.Center -> 1
            Constants.Gravity.Right -> 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val alignment = prefs.homeAlignment
        val layoutRes = when (viewType) {
            0 -> R.layout.item_app_left
            1 -> R.layout.item_app_center
            2 -> R.layout.item_app_right
            else -> R.layout.item_app_left
        }
        
        android.util.Log.d("HomeAppsAdapter", "Creating ViewHolder with alignment: $alignment, viewType: $viewType, layout: $layoutRes")
        
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appName = prefs.getAppName(position)
        holder.textView.text = if (appName.isEmpty()) "..." else appName

        // Style from prefs
        holder.textView.textSize = prefs.appSize.toFloat()
        
        // Auto text color based on wallpaper brightness or manual selection
        val textColor = if (prefs.autoTextColor) {
            app.wazabe.mlauncher.helper.utils.WallpaperColorAnalyzer.getRecommendedTextColor(holder.itemView.context)
        } else {
            if (prefs.manualTextColor == "light") {
                androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.white)
            } else {
                androidx.core.content.ContextCompat.getColor(holder.itemView.context, R.color.black)
            }
        }
        holder.textView.setTextColor(textColor)

        // Icon
        val appModel = prefs.getHomeAppModel(position)
        val icon = IconPackHelper.getSafeAppIcon(
            context = holder.itemView.context,
            packageName = appModel.activityPackage,
            useIconPack = prefs.useIconPack,
            iconPackTarget = IconCacheTarget.HOME
        )
        holder.appIcon.setImageDrawable(icon)
        
        // Vertical padding
        val density = holder.itemView.context.resources.displayMetrics.density
        val verticalPadding = (prefs.textPaddingSize * density).toInt()
        val horizontalPadding = (16 * density).toInt()
        holder.itemView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        holder.itemView.setOnClickListener { onClick(position) }
        holder.itemView.setOnLongClickListener { onLongClick(position); true }
    }

    override fun getItemCount() = prefs.homeAppsNum
}