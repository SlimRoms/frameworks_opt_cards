/*
 * ******************************************************************************
 *   Copyright (c) 2013-2014 Gabriele Mariotti.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  *****************************************************************************
 */

package com.android.cards.internal;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.android.cards.R;
import com.android.cards.internal.base.BaseCardCursorAdapter;
import com.android.cards.view.CardListView;
import com.android.cards.view.CardView;

/**
 * Cursor Adapter for {@link com.android.cards.internal.Card} model
 *
 *
 * </p>
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public abstract class CardCursorAdapter extends BaseCardCursorAdapter  {

    protected static String TAG = "CardCursorAdapter";

    /**
     * {@link com.android.cards.view.CardListView}
     */
    protected CardListView mCardListView;

    /**
     * Internal Map with all Cards.
     * It uses the card id value as key.
     */
    protected HashMap<String /* id */,Card>  mInternalObjects;


    protected final List<String> mExpandedIds;

    /**
     * Recycle
     */
    private boolean recycle = false;
    // -------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------

    public CardCursorAdapter(Context context) {
        super(context, null, false);
        mContext= context;
        mExpandedIds = new ArrayList<String>();
    }

    protected CardCursorAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        mContext= context;
        mExpandedIds = new ArrayList<String>();
    }

    protected CardCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mContext= context;
        mExpandedIds = new ArrayList<String>();
    }

    // -------------------------------------------------------------
    // Views
    // -------------------------------------------------------------


    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //Check for recycle
        if (convertView == null) {
            recycle = false;
        } else {
            recycle = true;
        }
        return super.getView(position, convertView, parent);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        int layout = mRowLayoutId;
        LayoutInflater mInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return mInflater.inflate(layout, parent, false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {

        CardView mCardView;
        Card mCard;

        mCard = (Card) getCardFromCursor(cursor);
        if (mCard != null) {
            mCardView = (CardView) view.findViewById(R.id.list_cardId);
            if (mCardView != null) {
                //It is important to set recycle value for inner layout elements
                mCardView.setForceReplaceInnerLayout(Card.equalsInnerLayout(mCardView.getCard(),mCard));

                //It is important to set recycle value for performance issue
                mCardView.setRecycle(recycle);

                //Save original swipeable to prevent cardSwipeListener (listView requires another cardSwipeListener)
                boolean origianlSwipeable = mCard.isSwipeable();
                mCard.setSwipeable(false);

                mCard.setExpanded(isExpanded(mCard));

                mCardView.setCard(mCard);

                //Set originalValue
                //mCard.setSwipeable(origianlSwipeable);
                if (origianlSwipeable)
                    Log.d(TAG, "Swipe action not enabled in this type of view");

                //If card has an expandable button override animation
                if ((mCard.getCardHeader() != null && mCard.getCardHeader().isButtonExpandVisible()) || mCard.getViewToClickToExpand()!=null ){
                    setupExpandCollapseListAnimation(mCardView);
                }

                //Setup swipeable animation
                setupSwipeableAnimation(mCard, mCardView);

            }
        }
    }


    /**
     * Sets SwipeAnimation on List
     *
     * @param card {@link com.android.cards.internal.Card}
     * @param cardView {@link com.android.cards.view.CardView}
     */
    protected void setupSwipeableAnimation(final Card card, CardView cardView) {

        cardView.setOnTouchListener(null);
    }

    /**
     * Overrides the default collapse/expand animation in a List
     *
     * @param cardView {@link com.android.cards.view.CardView}
     */
    protected void setupExpandCollapseListAnimation(CardView cardView) {

        if (cardView == null) return;
        cardView.setOnExpandListAnimatorListener(mCardListView);
    }


    // -------------------------------------------------------------
    //  Expanded
    // -------------------------------------------------------------

    /**
     *  Set the card as Expanded.
     */
    public void setExpanded(Card card) {
        if (card!=null)
            setExpanded(card.getId());
    }

    /**
     *  Set the card as Expanded using its id
     */
    public void setExpanded(final String id) {
        if (mExpandedIds!=null){
            if (mExpandedIds.contains(id)) {
                return;
            }
            mExpandedIds.add(id);
        }
    }


    /**
     *  Set the card as collapsed
     */
    public void setCollapsed(Card card) {
        if (card!=null)
            setCollapsed(card.getId());
    }

    /**
     *  Set the card as collapsed using its id
     */
    public void setCollapsed(final String id) {
        if (mExpandedIds!=null){
            if (!mExpandedIds.contains(id)) {
                return;
            }
            mExpandedIds.remove(id);
        }
    }


    /**
     * Indicates if the item at the specified position is expanded.
     *
     * @param card
     * @return true if the view is expanded, false otherwise.
     */
    public boolean isExpanded(Card card) {
        String itemId = card.getId();
        return mExpandedIds.contains(itemId);
    }

    /**
     * Checks if the item is already expanded
     *
     * @param viewCard
     * @return
     */
    public boolean onExpandStart(CardView viewCard) {
        Card card = viewCard.getCard();
        if (card!=null){
            String itemId = card.getId();
            if (!mExpandedIds.contains(itemId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * * Checks if the item is already collapsed
     *
     * @param viewCard
     * @return
     */
    public boolean onCollapseStart(CardView viewCard) {
        Card card = viewCard.getCard();
        if (card!=null){
            String itemId = card.getId();
            if (mExpandedIds.contains(itemId)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the mExpandedIds after an expand action
     *
     * @param viewCard
     */
    public void onExpandEnd(CardView viewCard) {
        Card card = viewCard.getCard();
        if (card!=null){
            setExpanded(card);
        }
    }

    /**
     * Updates the mExpandedIds after a collapse action
     *
     * @param viewCard
     */
    public void onCollapseEnd(CardView viewCard) {
        Card card = viewCard.getCard();
        if (card!=null){
            setCollapsed(card);
        }
    }

    // -------------------------------------------------------------
    //  Getters and Setters
    // -------------------------------------------------------------

    /**
     * @return {@link com.android.cards.view.CardListView}
     */
    public CardListView getCardListView() {
        return mCardListView;
    }

    /**
     * Sets the {@link com.android.cards.view.CardListView}
     *
     * @param cardListView cardListView
     */
    public void setCardListView(CardListView cardListView) {
        this.mCardListView = cardListView;
    }


    /**
     * Returns the expanded ids
     *
     * @return
     */
    public List<String> getExpandedIds() {
        return mExpandedIds;
    }
}
