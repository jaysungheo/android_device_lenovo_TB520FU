/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package com.lenovo.pen.cap.base;

import android.app.Activity;
import android.content.res.Resources;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

/**
 * The PixelOS card groups of the pen settings pages (see
 * BasePreferenceFragmentCompat) for the ZUI 18.5 app selection pages
 * (BaseAppSelectFragment: writing feedback apps, palm rejection apps). They
 * are a plain RecyclerView (rv_app_list) of category headers
 * (tv_category_title) and app rows instead of a preference list: the app rows
 * between two headers form one group of cards, the headers stay on the page
 * background with the category font. Attached by PageStyler once the list is
 * in the window (the fragment is added after the activity is created).
 */
final class AppListCards extends RecyclerView.ItemDecoration {
    final BasePreferenceFragmentCompat.CardDecoration mCards;
    final int mHeaderId;

    AppListCards(RecyclerView list, int headerId) {
        mCards = new BasePreferenceFragmentCompat.CardDecoration(list.getContext());
        mHeaderId = headerId;
    }

    static void attach(Activity activity) {
        Resources res = activity.getResources();
        String pkg = activity.getPackageName();
        final int listId = res.getIdentifier("rv_app_list", "id", pkg);
        final int headerId = res.getIdentifier("tv_category_title", "id", pkg);
        if (listId == 0) {
            return;
        }
        final View decor = activity.getWindow().getDecorView();
        decor.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        View list = decor.findViewById(listId);
                        if (!(list instanceof RecyclerView)) {
                            return;
                        }
                        decor.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        RecyclerView recycler = (RecyclerView) list;
                        recycler.setVerticalScrollBarEnabled(false);
                        recycler.addItemDecoration(new AppListCards(recycler, headerId));
                    }
                });
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
            RecyclerView.State state) {
        outRect.setEmpty();
        RecyclerView.Adapter adapter = parent.getAdapter();
        int position = parent.getChildAdapterPosition(view);
        if (adapter == null || position == RecyclerView.NO_POSITION) {
            return;
        }
        mCards.observe(parent, adapter);
        if (mHeaderId != 0 && view.getId() == mHeaderId) {
            if (view instanceof TextView
                    && ((TextView) view).getTypeface() != mCards.mCategoryFont) {
                ((TextView) view).setTypeface(mCards.mCategoryFont);
            }
            return;
        }
        BasePreferenceFragmentCompat.CardDecoration.font(view, android.R.id.title,
                mCards.mTitleFont);
        BasePreferenceFragmentCompat.CardDecoration.font(view, android.R.id.summary,
                mCards.mSummaryFont);
        if (view.getMinimumHeight() < mCards.mRowMinHeight) {
            view.setMinimumHeight(mCards.mRowMinHeight);
        }
        // Only headers and app rows: a neighbour of another type is a header.
        int type = adapter.getItemViewType(position);
        boolean first = position == 0 || adapter.getItemViewType(position - 1) != type;
        boolean last = position == adapter.getItemCount() - 1
                || adapter.getItemViewType(position + 1) != type;
        int shape = first
                ? (last ? BasePreferenceFragmentCompat.CardDecoration.SINGLE
                        : BasePreferenceFragmentCompat.CardDecoration.TOP)
                : (last ? BasePreferenceFragmentCompat.CardDecoration.BOTTOM
                        : BasePreferenceFragmentCompat.CardDecoration.MIDDLE);
        if (!first) {
            outRect.top = mCards.mGap;
        }
        Drawable background = view.getBackground();
        if (!(background instanceof BasePreferenceFragmentCompat.Card)
                || ((BasePreferenceFragmentCompat.Card) background).mShape != shape) {
            view.setBackground(mCards.card(shape));
        }
    }
}
