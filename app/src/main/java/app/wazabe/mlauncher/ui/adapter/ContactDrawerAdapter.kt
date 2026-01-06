package app.wazabe.mlauncher.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.droidworksstudio.common.AppLogger
import com.github.droidworksstudio.fuzzywuzzy.FuzzyFinder
import app.wazabe.mlauncher.R
import app.wazabe.mlauncher.data.Constants
import app.wazabe.mlauncher.data.ContactListItem
import app.wazabe.mlauncher.data.Prefs
import java.text.Normalizer

class ContactDrawerAdapter(
    private val context: Context,
    private val gravity: Int,
    private val contactClickListener: (ContactListItem) -> Unit
) : RecyclerView.Adapter<ContactDrawerAdapter.ViewHolder>(), Filterable {

    private lateinit var prefs: Prefs
    private var contactFilter = createContactFilter()
    var contactsList: MutableList<ContactListItem> = mutableListOf()
    var contactFilteredList: MutableList<ContactListItem> = mutableListOf()

    override fun getItemViewType(position: Int): Int {
        return when (prefs.drawerAlignment) {
            Constants.Gravity.Left -> 0
            Constants.Gravity.Center -> 1
            Constants.Gravity.Right -> 2
            Constants.Gravity.IconOnly -> 1
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        prefs = Prefs(parent.context)
        val layoutRes = when (viewType) {
            0 -> R.layout.item_app_left
            1 -> R.layout.item_app_center
            2 -> R.layout.item_app_right
            else -> R.layout.item_app_left
        }
        val view = LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
        return ViewHolder(view)
    }

    fun getItemAt(position: Int): ContactListItem? {
        return if (position in contactsList.indices) contactsList[position] else null
    }

    @SuppressLint("RecyclerView")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (contactFilteredList.isEmpty() || position !in contactFilteredList.indices) return

        val contactModel = contactFilteredList[holder.absoluteAdapterPosition]
        holder.bind(contactModel, contactClickListener, prefs)
    }

    override fun getItemCount(): Int = contactFilteredList.size

    override fun getFilter(): Filter = this.contactFilter

    private fun createContactFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(charSearch: CharSequence?): FilterResults {
                prefs = Prefs(context)

                val searchChars = charSearch.toString().trim().lowercase()
                val filteredContacts: MutableList<ContactListItem>

                // Normalization function for contacts
                val normalizeField: (ContactListItem) -> String = { contact -> normalize(contact.displayName) }

                // Scoring logic
                val scoredContacts: Map<ContactListItem, Int> = if (prefs.enableFilterStrength) {
                    contactsList.associateWith { contact ->
                        FuzzyFinder.scoreContact(contact, searchChars, Constants.MAX_FILTER_STRENGTH)
                    }
                } else {
                    emptyMap()
                }

                filteredContacts = if (searchChars.isEmpty()) {
                    contactsList.toMutableList()
                } else {
                    if (prefs.enableFilterStrength) {
                        // Filter using scores
                        scoredContacts.filter { (contact, score) ->
                            (prefs.searchFromStart && normalizeField(contact).startsWith(searchChars)
                                    || !prefs.searchFromStart && normalizeField(contact).contains(searchChars))
                                    && score > prefs.filterStrength
                        }.map { it.key }.toMutableList()
                    } else {
                        // Filter without scores
                        contactsList.filter { contact ->
                            if (prefs.searchFromStart) {
                                normalizeField(contact).startsWith(searchChars)
                            } else {
                                FuzzyFinder.isMatch(normalizeField(contact), searchChars)
                            }
                        }.toMutableList()
                    }
                }

                if (searchChars.isNotEmpty()) AppLogger.d("searchQuery", searchChars)

                val filterResults = FilterResults()
                filterResults.values = filteredContacts
                return filterResults
            }

            fun normalize(input: String): String {
                val temp = Normalizer.normalize(input, Normalizer.Form.NFC)
                return temp
                    .lowercase()
                    .filter { it.isLetterOrDigit() }
            }

            @SuppressLint("NotifyDataSetChanged")
            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results?.values is MutableList<*>) {
                    contactFilteredList = results.values as MutableList<ContactListItem>
                    notifyDataSetChanged()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setContactList(contactsList: MutableList<ContactListItem>) {
        this.contactsList = contactsList
        this.contactFilteredList = contactsList
        notifyDataSetChanged()
    }

    fun launchFirstInList() {
        if (contactFilteredList.isNotEmpty()) {
            contactClickListener(contactFilteredList[0])
        }
    }

    fun getFirstInList(): String? {
        return if (contactFilteredList.isNotEmpty()) {
            contactFilteredList[0].displayName
        } else {
            null
        }
    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val appTitle: TextView = view.findViewById(R.id.appTitle)

        fun bind(
            contactItem: ContactListItem,
            contactClickListener: (ContactListItem) -> Unit,
            prefs: Prefs
        ) {
            appTitle.text = contactItem.displayName
            appTitle.textSize = prefs.appSize.toFloat()
            // appTitle.setTextColor(prefs.appColor) - Using system default
            
            if (app.wazabe.mlauncher.Mlauncher.prefs.launcherFont != "system") {
                appTitle.typeface = app.wazabe.mlauncher.Mlauncher.globalTypeface
            }

            itemView.setOnClickListener {
                contactClickListener(contactItem)
            }
        }
    }
}
