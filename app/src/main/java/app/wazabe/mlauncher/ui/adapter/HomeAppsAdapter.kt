import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.Prefs
import app.wazabe.mlauncher.helper.IconCacheTarget
import app.wazabe.mlauncher.helper.IconPackHelper
import com.github.droidworksstudio.common.AppLogger

class HomeAppsAdapter(
    private val prefs: Prefs,
    private val onClick: (Int) -> Unit,
    private val onLongClick: (Int) -> Unit
) : RecyclerView.Adapter<HomeAppsAdapter.ViewHolder>() {

    //var itemCount: Int = 0 // Le nombre d'apps configuré dans les réglages

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView = view.findViewById(R.id.appLabel)
        val appIcon: ImageView = view.findViewById(R.id.appIconLeft)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Vous pouvez utiliser un layout simple ou le créer par code
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_home_app, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val appName = prefs.getAppName(position)
        holder.textView.text = if (appName.isEmpty()) "..." else appName

        // Application du style (taille, couleur) depuis les prefs
        holder.textView.textSize = prefs.appSize.toFloat()
        
        // Auto text color based on wallpaper brightness
        val textColor = if (prefs.autoTextColor) {
            app.wazabe.mlauncher.helper.utils.WallpaperColorAnalyzer.getRecommendedTextColor(holder.itemView.context)
        } else {
            prefs.appColor
        }
        holder.textView.setTextColor(textColor)

        // Gestion de l'icône (Compound Drawable)
        val appModel = prefs.getHomeAppModel(position)
        val icon = IconPackHelper.getSafeAppIcon(
            context = holder.itemView.context,
            packageName = appModel.activityPackage,
            useIconPack = prefs.useIconPack,
            iconPackTarget = IconCacheTarget.HOME // C'est ici qu'était l'erreur
        )
        holder.appIcon.setImageDrawable(icon)
        
        // Vertical padding requested by the user
        val density = holder.itemView.context.resources.displayMetrics.density
        val verticalPadding = (prefs.textPaddingSize * density).toInt()
        val horizontalPadding = (16 * density).toInt() // Preserve 16dp horizontal padding
        holder.itemView.setPadding(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding)

        if (position == 0) {
            //android.widget.Toast.makeText(holder.itemView.context, "Padding: $verticalPadding px (${prefs.textPaddingSize} dp)", android.widget.Toast.LENGTH_SHORT).show()
        }

        holder.itemView.setOnClickListener { onClick(position) }
        holder.itemView.setOnLongClickListener { onLongClick(position); true }
    }

    override fun getItemCount() = prefs.homeAppsNum
}