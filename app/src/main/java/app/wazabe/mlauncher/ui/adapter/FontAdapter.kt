package app.wazabe.mlauncher.ui.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import app.wazabe.mlauncher.R

class FontAdapter(
    context: Context,
    private val fontEntries: List<String>,
    private val fontValues: List<String>
) : ArrayAdapter<String>(context, android.R.layout.simple_list_item_1, fontEntries) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        val fontPath = fontValues[position]
        
        if (fontPath.equals("system", ignoreCase = true)) {
            view.typeface = Typeface.DEFAULT
        } else {
            try {
                view.typeface = Typeface.createFromAsset(context.assets, fontPath)
            } catch (e: Exception) {
                view.typeface = Typeface.DEFAULT
            }
        }
        
        // Add some padding and adjust text size for better preview
        view.textSize = 18f
        val padding = (16 * context.resources.displayMetrics.density).toInt()
        view.setPadding(padding, padding / 2, padding, padding / 2)
        
        return view
    }
}
