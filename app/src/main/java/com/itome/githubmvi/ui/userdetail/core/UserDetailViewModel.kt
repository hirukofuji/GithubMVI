package com.itome.githubmvi.ui.userdetail.core

import com.itome.githubmvi.mvibase.MviViewModel
import com.itome.githubmvi.ui.userdetail.core.UserDetailAction.*
import com.itome.githubmvi.ui.userdetail.core.UserDetailIntent.*
import com.itome.githubmvi.ui.userdetail.core.UserDetailResult.*
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import javax.inject.Inject

class UserDetailViewModel @Inject constructor(
    private val actionProcessorHolder: UserDetailProcessorHolder
) : MviViewModel<UserDetailIntent, UserDetailViewState> {

    private val intentsSubject: PublishSubject<UserDetailIntent> = PublishSubject.create()
    private val statesObservable: Observable<UserDetailViewState> = compose()

    override fun processIntents(intents: Observable<UserDetailIntent>) {
        intents.subscribe(intentsSubject)
    }

    override fun states(): Observable<UserDetailViewState> = statesObservable

    private fun compose(): Observable<UserDetailViewState> {
        return intentsSubject
            .map(this::actionFromIntent)
            .compose(actionProcessorHolder.actionProcessor)
            .scan(UserDetailViewState.idle(), reducer)
            .replay(1)
            .autoConnect(0)
    }

    private fun actionFromIntent(intent: UserDetailIntent): UserDetailAction {
        return when (intent) {
            is FetchUserIntent -> FetchUserAction(intent.userName)
            is FetchUserReposIntent -> FetchUserReposAction(intent.userName)
            is CheckIsLoginUserIntent -> CheckIsLoginUserAction(intent.userName)
            is CheckIsFollowedIntent -> CheckIsFollowedAction(intent.userName)
            is FollowIntent -> FollowAction(intent.userName)
            is UnFollowIntent -> UnFollowAction(intent.userName)
        }
    }

    companion object {
        private val reducer = { previousState: UserDetailViewState, result: UserDetailResult ->
            when (result) {
                is FetchUserResult -> when (result) {
                    is FetchUserResult.Success ->
                        previousState.copy(
                            user = result.user,
                            error = null,
                            isLoading = false
                        )
                    is FetchUserResult.Failure ->
                        previousState.copy(error = result.error, isLoading = false)
                    FetchUserResult.InFlight ->
                        previousState.copy(isLoading = true)
                }

                is FetchUserReposResult -> when (result) {
                    is FetchUserReposResult.Success ->
                        previousState.copy(
                            repos = result.repos,
                            error = null,
                            isLoading = false
                        )
                    is FetchUserReposResult.Failure ->
                        previousState.copy(error = result.error, isLoading = false)
                    FetchUserReposResult.InFlight ->
                        previousState.copy(isLoading = true)
                }

                is CheckIsLoginUserResult -> when (result) {
                    is CheckIsLoginUserResult.Success ->
                        previousState.copy(
                            isLoginUser = result.isLoginUser,
                            error = null,
                            isLoading = false
                        )
                    is CheckIsLoginUserResult.Failure ->
                        previousState.copy(error = result.error, isLoading = false)
                    CheckIsLoginUserResult.InFlight ->
                        previousState.copy(isLoading = true)
                }

                is CheckIsFollowedResult -> when (result) {
                    is CheckIsFollowedResult.Success ->
                        previousState.copy(
                            isFollowed = result.isFollowed,
                            error = null,
                            isLoading = false
                        )
                    is CheckIsFollowedResult.Failure ->
                        previousState.copy(error = result.error, isLoading = false)
                    CheckIsFollowedResult.InFlight ->
                        previousState.copy(isLoading = true)
                }

                is FollowResult -> when (result) {
                    FollowResult.Success -> previousState.copy(
                        isFollowed = true,
                        user = previousState.user?.plusFollowerCount()
                    )
                    is FollowResult.Failure -> previousState.copy(error = result.error)
                }

                is UnFollowResult -> when (result) {
                    UnFollowResult.Success -> previousState.copy(
                        isFollowed = false,
                        user = previousState.user?.minusFollowerCount()
                    )
                    is UnFollowResult.Failure -> previousState.copy(error = result.error)
                }
            }
        }
    }
}