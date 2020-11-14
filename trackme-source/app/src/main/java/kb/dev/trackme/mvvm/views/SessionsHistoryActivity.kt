package kb.dev.trackme.mvvm.views

import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kb.dev.trackme.*
import kb.dev.trackme.database.Session
import kb.dev.trackme.mvvm.viewmodels.SessionsHistoryViewModel
import kb.dev.trackme.utils.SharePreferenceUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.viewmodel.ext.android.viewModel
import java.io.File

@ExperimentalCoroutinesApi
@FlowPreview
class SessionsHistoryActivity : AppCompatActivity() {
    private val pagingAdapter = SessionAdapter(SessionComparator)
    private val viewModel: SessionsHistoryViewModel by viewModel()
    private val sharedPreferences: SharePreferenceUtils by inject()

    object SessionComparator : DiffUtil.ItemCallback<Session>() {
        override fun areItemsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Session, newItem: Session): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val lastSessionState = sharedPreferences.getLastSessionSate()
        if (lastSessionState != null && lastSessionState != SessionState.COMPLETE.toString()) {
            startActivity(Intent(this, SessionActivity::class.java))
            finish()
        } else {
            setContentView(R.layout.activity_sessions_history)
            setupRecycleView()
            setupInteraction()

            lifecycleScope.launch {
                viewModel.flow.collectLatest { pagingData ->
                    pagingAdapter.submitData(pagingData)
                }
            }
        }
    }

    private fun setupInteraction() {
        findViewById<ImageView>(R.id.btnRecord).setOnClickListener {
            startActivity(Intent(this, SessionActivity::class.java))
            finish()
        }
    }

    private fun setupRecycleView() {
        val rcvSessions = findViewById<RecyclerView>(R.id.rcvSessions)
        rcvSessions?.layoutManager = LinearLayoutManager(this)
        rcvSessions.addItemDecoration(
            object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    super.getItemOffsets(outRect, view, parent, state)
                    val position = parent.getChildAdapterPosition(view)
                    val count = parent.adapter?.itemCount ?: 0
                    if(position == 0){
                        outRect.top = this@SessionsHistoryActivity.resources.getDimensionPixelOffset(
                            R.dimen.margin_normal
                        )
                    }
                    outRect.bottom = this@SessionsHistoryActivity.resources.getDimensionPixelOffset(
                        R.dimen.margin_normal
                    )
                    if (position == count - 1) {
                        outRect.bottom += this@SessionsHistoryActivity.resources.getDimensionPixelOffset(
                            R.dimen.action_button_size
                        )
                    }

                }
            }
        )

        rcvSessions?.adapter = pagingAdapter
    }

    class SessionAdapter(diffCallback: DiffUtil.ItemCallback<Session>) :
        PagingDataAdapter<Session, SessionViewHolder>(diffCallback) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
            return SessionViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.row_session, parent, false)
            )
        }

        override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
            val item = getItem(position)
            holder.bind(item)
        }
    }

    class SessionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(item: Session?) {
            item?.let { session: Session ->
                session.route?.let {
                    Glide.with(itemView.context).load(File(it)).centerCrop().thumbnail(0.25f)
                        .into(itemView.findViewById(R.id.imvRoute))
                }

                setDistance(
                    itemView.findViewById<TextView>(R.id.tvDistance),
                    session.distance.toDouble()
                )
                setDuration(
                    itemView.findViewById<TextView>(R.id.tvDuration),
                    session.duration.toDouble()
                )
                setVelocity(itemView.findViewById<TextView>(R.id.tvAvgSpeed), session.avgSpeed)
            }
        }
    }
}