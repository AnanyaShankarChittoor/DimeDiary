package  com.project.dimediaryapp.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class Expense(
    var id: String = "",
    var amount: Int = 0,
    var category: String = "",
    var description: String = "",
    var name: String = "",
    var userId: String = "",
    @ServerTimestamp var date: Date? = null
)
