/*
 * © 2016-2018 FlowCrypt Limited. Limitations apply. Contact human@flowcrypt.com
 * Contributors: DenBond7
 */

package com.flowcrypt.email.ui.adapter;

import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;

/**
 * A common application feature is to load automatically more items as the user scrolls through
 * the items (aka infinite scroll). This is done by triggering a request for more data once the
 * user crosses a threshold of remaining items before they've hit the end.
 * <p>
 * See Endless Scrolling with AdapterViews (https://github
 * .com/codepath/android_guides/wiki/Endless-Scrolling-with-AdapterViews-and-RecyclerView)
 *
 * @author DenBond7
 * Date: 23.06.2017
 * Time: 11:17
 * E-mail: DenBond7@gmail.com
 */

public abstract class EndlessScrollListener implements OnScrollListener {
  // The minimum number of items to have below your current scroll position
  // before loading more.
  private int visibleThreshold = 5;
  // The current offset index of data you have loaded
  private int currentPage;
  // The total number of items in the dataset after the last load
  private int previousTotalItemCount;
  // True if we are still waiting for the last set of data to load.
  private boolean loading = true;
  // Sets the starting page index
  private int startingPageIndex;

  public EndlessScrollListener() {
  }

  public EndlessScrollListener(int visibleThreshold) {
    this.visibleThreshold = visibleThreshold;
  }

  public EndlessScrollListener(int visibleThreshold, int startPage) {
    this.visibleThreshold = visibleThreshold;
    this.startingPageIndex = startPage;
    this.currentPage = startPage;
  }

  // Defines the process for actually loading more data based on page
  // Returns true if more data is being loaded; returns false if there is no more data to load.
  public abstract boolean onLoadMore(int page, int totalItemsCount);

  @Override
  public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int
      totalItemCount) {
    // If the total item count is zero and the previous isn't, assume the
    // list is invalidated and should be reset back to initial state
    if (totalItemCount < previousTotalItemCount) {
      this.currentPage = this.startingPageIndex;
      this.previousTotalItemCount = totalItemCount;
      if (totalItemCount == 0) {
        this.loading = true;
      }
    }
    // If it's still loading, we check to see if the dataset count has
    // changed, if so we conclude it has finished loading and update the current page
    // number and total item count.
    if (loading && (totalItemCount > previousTotalItemCount)) {
      loading = false;
      previousTotalItemCount = totalItemCount;
      currentPage++;
    }

    // If it isn't currently loading, we check to see if we have breached
    // the visibleThreshold and need to reload more data.
    // If we do need to reload some more data, we execute onLoadMore to fetch the data.
    if (!loading && (firstVisibleItem + visibleItemCount + visibleThreshold) >=
        totalItemCount) {
      loading = onLoadMore(currentPage + 1, totalItemCount);
    }
  }

  @Override
  public void onScrollStateChanged(AbsListView view, int scrollState) {
    // Don't take any action on changed
  }
}
