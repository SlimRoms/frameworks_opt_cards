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

import java.util.HashMap;

import com.android.cards.R;
import com.android.cards.internal.base.BaseCardCursorAdapter;
import com.android.cards.view.CardGridView;
import com.android.cards.view.CardView;

/**
 * Cursor Adapter for {@link Card} model
 *
 *
 * </p>
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public abstract class CardGridCursorAdapter extends BaseCardCursorAdapter  {

    protected static String TAG = "CardGridCursorAdapter";

    /**
     * {@link com.android.cards.view.CardGridView}
     */
    protected CardGridView mCardGridView;

    /**
     * Internal Map with all Cards.
     * It uses the card id value as key.
     */
    protected HashMap<String /* id */,Card>  mInternalObjects;

    /**
     * Recycle
     */
    private boolean recycle = false;
    // -------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------

    public CardGridCursorAdapter(Context context) {
        super(context, null, false);
        mContext= context;
    }

    protected CardGridCursorAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
        mContext= context;
    }

    protected CardGridCursorAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mContext= context;
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

                mCardView.setCard(mCard);

                //Set originalValue
                //mCard.setSwipeable(origianlSwipeable);
                if (origianlSwipeable)
                    Log.d(TAG, "Swipe action not enabled in this type of view");

                //If card has an expandable button override animation
                if (mCard.getCardHeader() != null && mCard.getCardHeader().isButtonExpandVisible()) {
                    //setupExpandCollapseListAnimation(mCardView);
                    Log.d(TAG, "Expand action not enabled in this type of view");
                }

                //Setup swipeable animation
                setupSwipeableAnimation(mCard, mCardView);

            }
        }
    }



    /**
     * Sets SwipeAnimation on List
     *
     * @param card {@link Card}
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
        cardView.setOnExpandListAnimatorListener(mCardGridView);
    }



    // -------------------------------------------------------------
    //  Getters and Setters
    // -------------------------------------------------------------

    /**
     * @return {@link com.android.cards.view.CardGridView}
     */
    public CardGridView getCardGridView() {
        return mCardGridView;
    }

    /**
     * Sets the {@link com.android.cards.view.CardListView}
     *
     * @param cardGridView cardGridView
     */
    public void setCardGridView(CardGridView cardGridView) {
        this.mCardGridView = cardGridView;
    }

    /**
     * Indicates if the undo message is enabled after a swipe action
     *
     * @return <code>true</code> if the undo message is enabled
     */
    /*public boolean isEnableUndo() {
        return mEnableUndo;
    }*/

    /**
     * Enables an undo message after a swipe action
     *
     * @param enableUndo <code>true</code> to enable an undo message
     */
    /*
    public void setEnableUndo(boolean enableUndo) {
        mEnableUndo = enableUndo;
        if (enableUndo) {
            mInternalObjects = new HashMap<String, Card>();
            for (int i=0;i<getCount();i++) {
                Card card = getItem(i);
                mInternalObjects.put(card.getId(), card);
            }

            //Create a UndoController
            if (mUndoBarController==null){
                View undobar = ((Activity)mContext).findViewById(R.id.list_card_undobar);
                if (undobar != null) {
                    mUndoBarController = new UndoBarController(undobar, this);
                }
            }
        }else{
            mUndoBarController=null;
        }
    }*/

    /**
     * Return the UndoBarController for undo action
     *
     * @return {@link com.android.cards.view.listener.UndoBarController}
     */
    /*
    public UndoBarController getUndoBarController() {
        return mUndoBarController;
    }*/
}
