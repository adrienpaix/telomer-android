package health.telomer.android.core.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "action_plan_checks")
data class ActionPlanCheck(
    @PrimaryKey val id: String, // plan_id + "_" + item_index + "_" + date
    val planId: String,
    val itemIndex: Int,
    val date: String, // YYYY-MM-DD
    val isChecked: Boolean,
)

@Dao
interface ActionPlanCheckDao {
    @Query("SELECT * FROM action_plan_checks WHERE planId = :planId AND date = :date")
    fun getChecks(planId: String, date: String): Flow<List<ActionPlanCheck>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertCheck(check: ActionPlanCheck)
}

@Database(entities = [ActionPlanCheck::class], version = 1, exportSchema = false)
abstract class TelomerDatabase : RoomDatabase() {
    abstract fun actionPlanCheckDao(): ActionPlanCheckDao
}
