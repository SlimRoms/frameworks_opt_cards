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

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.HashMap;
import java.util.List;

import com.android.cards.R;
import com.android.cards.internal.base.BaseCardArrayAdapter;
import com.android.cards.view.CardListView;
import com.android.cards.view.CardView;
import com.android.cards.view.listener.SwipeDismissListViewTouchListener;
import com.android.cards.view.listener.UndoBarController;
import com.android.cards.view.listener.UndoCard;

/**
 * Array Adapter for {@link Card} model
 * <p/>
 * Usage:
 * <pre><code>
 * ArrayList<Card> cards = new ArrayList<Card>();
 * for (int i=0;i<1000;i++){
 *     CardExample card = new CardExample(getActivity(),"My title "+i,"Inner text "+i);
 *     cards.add(card);
 * }
 *
 * CardArrayAdapter mCardArrayAdapter = new CardArrayAdapter(getActivity(),cards);
 *
 * CardListView listView = (CardListView) getActivity().findViewById(R.id.listId);
 * listView.setAdapter(mCardArrayAdapter); *
 * </code></pre>
 * It provides a default layout id for each row @layout/list_card_layout
 * Use can easily customize it using card:list_card_layout_resourceID attr in your xml layout:
 * <pre><code>
 *    <com.android.cards.view.CardListView
 *      android:layout_width="match_parent"
 *      android:layout_height="match_parent"
 *      android:id="@+id/carddemo_list_gplaycard"
 *      card:list_card_layout_resourceID="@layout/list_card_thumbnail_layout" />
 * </code></pre>
 * or:
 * <pre><code>
 * adapter.setRowLayoutId(list_card_layout_resourceID);
 * </code></pre>
 * </p>
 * @author Gabriele Mariotti (gabri.mariotti@gmail.com)
 */
public class CardArrayAdapter extends BaseCardArrayAdapter implements UndoBarController.UndoListener {

    protected static String TAG = "CardArrayAdapter";

    /**
     * {@link CardListView}
     */
    protected CardListView mCardListView;

    /**
     * Listener invoked when a card is swiped
     */
    protected SwipeDismissListViewTouchListener mOnTouchListener;

    /**
     * Used to enable an undo message after a swipe action
     */
    protected boolean mEnableUndo=false;

    /**
     * Undo Controller
     */
    protected UndoBarController mUndoBarController;

    /**
     * Internal Map with all Cards.
     * It uses the card id value as key.
     */
    protected HashMap<String /* id */,Card>  mInternalObjects;


    // -------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------

    /**
     * Constructor
     *
     * @param context The current context.
     * @param cards   The cards to represent in the ListView.
     */
    public CardArrayAdapter(Context context, List<Card> cards) {
        super(context, cards);
    }

    // -------------------------------------------------------------
    // Views
    // -------------------------------------------------------------

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        ViewHolder holder;

        // Retrieve card from items
        Card card = (Card) getItem(position);
        if (card != null) {
            int layout = mRowLayoutId;
            boolean recycle = false;

            // Inflate layout
            if (view == null) {
                recycle = false;
                LayoutInflater inflater =
                        (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(layout, parent, false);

                holder = new ViewHolder();
                holder.cardView = (CardView) view.findViewById(R.id.list_cardId);
                view.setTag(holder);
            } else {
                recycle = true;
                holder = (ViewHolder) view.getTag();
            }

            // Setup card
            CardView cardView = holder.cardView;
            if (cardView != null) {
                // It is important to set recycle value for inner layout elements
                cardView.setForceReplaceInnerLayout(
                        Card.equalsInnerLayout(cardView.getCard(), card));

                // It is important to set recycle value for performance issue
                cardView.setRecycle(recycle);

                // Save original swipeable to prevent cardSwipeListener
                // (listView requires another cardSwipeListener)
                boolean origianlSwipeable = card.isSwipeable();
                card.setSwipeable(false);

                cardView.setCard(card);

                // Set originalValue
                card.setSwipeable(origianlSwipeable);

                // If card has an expandable button override animation
                if ((card.getCardHeader() != null
                        && card.getCardHeader().isButtonExpandVisible())
                        || card.getViewToClickToExpand() != null) {
                    setupExpandCollapseListAnimation(cardView);
                }

                // Setup swipeable animation
                setupSwipeableAnimation(card, cardView);

                //setupMultiChoice
                setupMultichoice(view, card, cardView, position);
            }
        }

        return view;
    }

    static class ViewHolder {
        CardView cardView;
    }


    /**
     * Sets SwipeAnimation on List
     *
     * @param card {@link Card}
     * @param cardView {@link CardView}
     */
    protected void setupSwipeableAnimation(final Card card, CardView cardView) {
        HashMap<Integer, Card.OnLongCardClickListener> multipleOnLongClickListner =
                card.getMultipleOnLongClickListener();
        if (card.isSwipeable()){
            if (mOnTouchListener == null){
                mOnTouchListener = new SwipeDismissListViewTouchListener(mCardListView, mCallback);
                // Setting this scroll listener is required to ensure that during
                // ListView scrolling, we don't look for swipes.
                mCardListView.setOnScrollListener(mOnTouchListener.makeScrollListener());
            }

            cardView.setOnTouchListener(mOnTouchListener);

            // We may have partial onlongclicklistener. Restore onTouchListener for this views.
            setPartialOnTouchListeners(cardView, mOnTouchListener, multipleOnLongClickListner);
        } else {
            //prevent issue with recycle view
            cardView.setOnTouchListener(null);
            setPartialOnTouchListeners(cardView, null, multipleOnLongClickListner);
        }
    }

    private void setPartialOnTouchListeners(CardView cardView,
            SwipeDismissListViewTouchListener onTouchListener,
            HashMap<Integer, Card.OnLongCardClickListener> multipleOnLongClickListner) {
        if (multipleOnLongClickListner != null && !multipleOnLongClickListner.isEmpty()) {
            for (int key : multipleOnLongClickListner.keySet()) {
                View viewLongClickable = cardView.decodeAreaOnClickListener(key);
                if (viewLongClickable != null) {
                    viewLongClickable.setOnTouchListener(mOnTouchListener);
                }
            }
        }
    }

    /**
     * Overrides the default collapse/expand animation in a List
     *
     * @param cardView {@link CardView}
     */
    protected void setupExpandCollapseListAnimation(CardView cardView) {

        if (cardView == null) return;
        cardView.setOnExpandListAnimatorListener(mCardListView);
    }

    // -------------------------------------------------------------
    //  SwipeListener and undo action
    // -------------------------------------------------------------
    /**
     * Listener invoked when a card is swiped
     */
    SwipeDismissListViewTouchListener.DismissCallbacks mCallback = new SwipeDismissListViewTouchListener.DismissCallbacks() {

        @Override
        public boolean canDismiss(int position, Card card) {
            return card.isSwipeable();
        }

        @Override
        public void onDismiss(ListView listView, int[] reverseSortedPositions) {

            int[] itemPositions=new int[reverseSortedPositions.length];
            String[] itemIds=new String[reverseSortedPositions.length];
            int i=0;

            //Remove cards and notifyDataSetChanged
            for (int position : reverseSortedPositions) {
                Card card = getItem(position);
                itemPositions[i]=position;
                itemIds[i]=card.getId();
                i++;

                remove(card);
                if (card.getOnSwipeListener() != null){
                        card.getOnSwipeListener().onSwipe(card);
                }
            }
            notifyDataSetChanged();

            //Check for a undo message to confirm
            if (isEnableUndo() && mUndoBarController!=null){

                //Show UndoBar
                UndoCard itemUndo=new UndoCard(itemPositions,itemIds);

                if (getContext()!=null){
                    Resources res = getContext().getResources();
                    if (res!=null){
                        String messageUndoBar = res.getQuantityString(R.plurals.list_card_undo_items, reverseSortedPositions.length, reverseSortedPositions.length);

                        mUndoBarController.showUndoBar(
                                false,
                                messageUndoBar,
                                itemUndo);
                    }
                }

            }
        }
    };

    // -------------------------------------------------------------
    //  Undo Default Listener
    // -------------------------------------------------------------

    @Override
    public void onUndo(Parcelable token) {
        //Restore items in lists (use reverseSortedOrder)
        if (token != null) {

            UndoCard item = (UndoCard) token;
            int[] itemPositions = item.itemPosition;
            String[] itemIds = item.itemId;

            if (itemPositions != null) {
                int end = itemPositions.length;

                for (int i = end - 1; i >= 0; i--) {
                    int itemPosition = itemPositions[i];
                    String id= itemIds[i];

                    if (id==null){
                        Log.w(TAG, "You have to set a id value to use the undo action");
                    }else{
                        Card card = mInternalObjects.get(id);
                        if (card!=null){
                            insert(card, itemPosition);
                            notifyDataSetChanged();
                            if (card.getOnUndoSwipeListListener()!=null)
                                card.getOnUndoSwipeListListener().onUndoSwipe(card);
                        }
                    }
                }
            }
        }
    }

    /**
     * Indicates if the undo message is enabled after a swipe action
     *
     * @return <code>true</code> if the undo message is enabled
     */
    public boolean isEnableUndo() {
        return mEnableUndo;
    }

    /**
     * Enables an undo message after a swipe action
     *
     * @param enableUndo <code>true</code> to enable an undo message
     */
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

                if (mUndoBarUIElements==null)
                    mUndoBarUIElements=new UndoBarController.DefaultUndoBarUIElements();

                View undobar = ((Activity)mContext).findViewById(mUndoBarUIElements.getUndoBarId());
                if (undobar != null) {
                    mUndoBarController = new UndoBarController(undobar, this,mUndoBarUIElements);
                }
            }
        }else{
            mUndoBarController=null;
        }
    }

    // -------------------------------------------------------------
    //  Getters and Setters
    // -------------------------------------------------------------

    /**
     * @return {@link CardListView}
     */
    public CardListView getCardListView() {
        return mCardListView;
    }

    /**
     * Sets the {@link CardListView}
     *
     * @param cardListView cardListView
     */
    public void setCardListView(CardListView cardListView) {
        this.mCardListView = cardListView;
    }



    /**
     * Return the UndoBarController for undo action
     *
     * @return {@link UndoBarController}
     */
    public UndoBarController getUndoBarController() {
        return mUndoBarController;
    }
}
