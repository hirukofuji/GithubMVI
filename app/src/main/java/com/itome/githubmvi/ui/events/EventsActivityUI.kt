package com.itome.githubmvi.ui.events

import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.itome.githubmvi.R
import com.itome.githubmvi.extensions.getContextColor
import com.itome.githubmvi.ui.circleImageView
import com.itome.githubmvi.ui.events.core.EventsViewState
import io.reactivex.subjects.PublishSubject
import org.jetbrains.anko.AnkoComponent
import org.jetbrains.anko.AnkoContext
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.design.appBarLayout
import org.jetbrains.anko.dip
import org.jetbrains.anko.linearLayout
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.jetbrains.anko.support.v4.swipeRefreshLayout
import org.jetbrains.anko.textColor
import org.jetbrains.anko.textView
import org.jetbrains.anko.verticalLayout
import org.jetbrains.anko.wrapContent


class EventsActivityUI : AnkoComponent<EventsActivity> {

    private val eventsAdapter = EventsAdapter()

    val userImageClickPublisher = eventsAdapter.userImageClickPublisher
    val itemViewClickPublisher = eventsAdapter.itemViewClickPublisher
    val refreshPublisher = PublishSubject.create<View>()!!
    val loadMorePublisher = PublishSubject.create<Int>()!!
    val loginUserImageClickPublisher = PublishSubject.create<Pair<String, View>>()!!

    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var userCircleImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var endlessOnScrollListener: EndlessOnScrollListener

    fun applyState(state: EventsViewState) {
        if (!state.isLoading && state.nextPage == 2) { // when first page loaded
            endlessOnScrollListener.resetListener()
        }
        eventsAdapter.events = state.events
        eventsAdapter.notifyDataSetChanged()
        swipeRefreshLayout.isRefreshing = state.isLoading

        Glide.with(userCircleImageView)
            .load(state.loginUser?.avatar_url)
            .apply(RequestOptions().placeholder(R.color.gray))
            .into(userCircleImageView)
        userNameTextView.text = state.loginUser?.login
        userCircleImageView.setOnClickListener {
            state.loginUser?.let {
                loginUserImageClickPublisher.onNext(state.loginUser.login to userCircleImageView)
            }
        }
    }

    override fun createView(ui: AnkoContext<EventsActivity>) = with(ui) {
        verticalLayout {
            appBarLayout {
                toolbar {
                    linearLayout {
                        userCircleImageView = circleImageView {
                            transitionName = "userImage"
                        }.lparams(dip(32), dip(32))
                        userNameTextView = textView {
                            textColor = context.getContextColor(R.color.white)
                            textSize = 24F
                        }.lparams(wrapContent, wrapContent) {
                            leftMargin = dip(16)
                        }
                    }
                }
            }

            swipeRefreshLayout = swipeRefreshLayout {
                setOnRefreshListener { refreshPublisher.onNext(this) }
                recyclerView {
                    layoutManager =
                            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                    adapter = eventsAdapter
                    endlessOnScrollListener = object : EndlessOnScrollListener(
                        layoutManager as LinearLayoutManager
                    ) {
                        override fun onLoadMore(page: Int) {
                            loadMorePublisher.onNext(page)
                        }
                    }

                    addOnScrollListener(endlessOnScrollListener)
                }
            }
        }
    }

    abstract inner class EndlessOnScrollListener(
        private val linearLayoutManager: LinearLayoutManager
    ) : RecyclerView.OnScrollListener() {

        private val visibleThreshold = 5
        private var previousTotal = 0
        private var loading = true
        private var currentPage = 1

        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val visibleItemCount = recyclerView!!.childCount
            val totalItemCount = linearLayoutManager.itemCount
            val firstVisibleItem = linearLayoutManager.findFirstVisibleItemPosition()

            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false
                    previousTotal = totalItemCount
                }
            }
            if (!loading && totalItemCount - visibleItemCount <= firstVisibleItem + visibleThreshold) {
                currentPage++
                onLoadMore(currentPage)
                loading = true
            }
        }

        fun resetListener() {
            loading = true
            previousTotal = 0
            currentPage = 1
        }

        abstract fun onLoadMore(page: Int)
    }
}
