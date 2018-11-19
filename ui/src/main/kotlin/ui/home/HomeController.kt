package tachiyomi.ui.home

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import com.bluelinelabs.conductor.Controller
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.Router
import com.google.android.material.behavior.HideBottomViewOnScrollBehavior
import kotlinx.android.synthetic.main.home_controller_bottomnav.*
import kotlinx.android.synthetic.main.home_controller_drawer.*
import tachiyomi.app.R
import tachiyomi.core.di.AppScope
import tachiyomi.prefs.UiPreferences
import tachiyomi.ui.base.BaseController
import tachiyomi.ui.base.withFadeTransition
import tachiyomi.ui.base.withoutTransition
import tachiyomi.ui.catalogs.CatalogsController
import tachiyomi.ui.library.LibraryController
import javax.inject.Inject

class HomeController : BaseController() {

  @Inject
  lateinit var uiPrefs: UiPreferences

  var usesDrawer = false
    private set

  var childRouter: Router? = null
    private set

  private val childRouterChangeListener = HomeControllerChangeListener()

  init {
    AppScope.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup,
    savedViewState: Bundle?
  ): View {
    usesDrawer = uiPrefs.useDrawer().get()
    val layout = if (usesDrawer) {
      R.layout.home_controller_drawer
    } else {
      R.layout.home_controller_bottomnav
    }
    return inflater.inflate(layout, container, false)
  }

  override fun onViewCreated(view: View) {
    super.onViewCreated(view)

    val router = getChildRouter(view.findViewById(R.id.home_controller_container))
    childRouter = router
    router.addChangeListener(childRouterChangeListener)

    home_nav_view?.setNavigationItemSelectedListener { onSetSelectedItem(it.itemId, router) }
    home_bottomnav?.setOnNavigationItemSelectedListener { onSetSelectedItem(it.itemId, router) }

    if (!router.hasRootController()) {
      performSetSelectedItem(R.id.nav_drawer_catalogues)
    }
  }

  override fun onDestroyView(view: View) {
    home_nav_view?.setNavigationItemSelectedListener(null)
    home_bottomnav?.setOnNavigationItemSelectedListener(null)
    childRouter?.removeChangeListener(childRouterChangeListener)
    childRouter = null
    super.onDestroyView(view)
  }

  private fun performSetSelectedItem(itemId: Int) {
    home_nav_view?.setCheckedItem(itemId)
    home_nav_view?.menu?.performIdentifierAction(itemId, 0)
    home_bottomnav?.selectedItemId = itemId
  }

  private fun onSetSelectedItem(id: Int, router: Router): Boolean {
    val currentRoot = router.backstack.firstOrNull()
    if (currentRoot?.tag()?.toIntOrNull() != id) {
      when (id) {
        R.id.nav_drawer_library -> setRoot(router, LibraryController(), id)
        R.id.nav_drawer_catalogues -> setRoot(router, CatalogsController(), id)
      }
    }
    home_drawer?.closeDrawer(GravityCompat.START)
    return true
  }

  private fun <HC> setRoot(
    router: Router,
    controller: HC,
    id: Int
  ) where HC : Controller, HC : HomeChildController {
    val transaction = if (!router.hasRootController()) {
      controller.withoutTransition()
    } else {
      controller.withFadeTransition()
    }
    router.setRoot(transaction.tag("$id"))
  }

  fun openDrawer() {
    home_drawer?.openDrawer(Gravity.START)
  }

  inner class HomeControllerChangeListener : ControllerChangeHandler.ControllerChangeListener {

    override fun onChangeStarted(
      to: Controller?,
      from: Controller?,
      isPush: Boolean,
      container: ViewGroup,
      handler: ControllerChangeHandler
    ) {
      if (from is HomeChildController.FAB) {
        view?.findViewById<ViewGroup>(R.id.home_fab_container)?.removeAllViews()
      }
      if (to is HomeChildController.FAB) {
        view?.findViewById<ViewGroup>(R.id.home_fab_container)?.let { it.addView(to.createFAB(it)) }
      }
      // If going back, restore bottom nav visibility, in case the new view can't scroll
      if (!isPush) {
        val nav = home_bottomnav
        if (nav != null) {
          val params = nav.layoutParams as CoordinatorLayout.LayoutParams
          val behavior = params.behavior as? HideBottomViewOnScrollBehavior
          behavior?.onNestedScroll(home_coordinator_bottomnav, nav, nav,
            0, -1, 0, 0, ViewCompat.TYPE_TOUCH)
        }
      }
    }

    override fun onChangeCompleted(
      to: Controller?,
      from: Controller?,
      isPush: Boolean,
      container: ViewGroup,
      handler: ControllerChangeHandler
    ) {

    }

  }

}