package com.itome.githubmvi.ui.splash

import com.itome.githubmvi.data.model.User
import com.itome.githubmvi.mvibase.MviResult

sealed class SplashResult : MviResult {

    sealed class FetchAccessTokenResult : SplashResult() {
        object Success : FetchAccessTokenResult()
        data class Failure(val error: Throwable) : FetchAccessTokenResult()
        object InFlight : FetchAccessTokenResult()
    }

    sealed class FetchLoginDataResult : SplashResult() {
        data class Success(val user: User) : FetchLoginDataResult()
        data class Failure(val error: Throwable) : FetchLoginDataResult()
        object NeedsAccessToken : FetchLoginDataResult()
        object InFlight : FetchLoginDataResult()
    }
}