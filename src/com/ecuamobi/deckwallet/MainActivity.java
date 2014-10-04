package com.ecuamobi.deckwallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.print.PrintHelper;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ecuamobi.deckwallet.qr.ErrorCorrectLevel;
import com.ecuamobi.deckwallet.qr.QRCode;
import com.ecuamobi.deckwallet.util.KeyPair;
import com.ecuamobi.deckwallet.util.Renderer;
import com.ecuamobi.deckwallet.util.Util;

public class MainActivity extends Activity {
	private static final String TAG = MainActivity.class.getName();
	protected static final int NUMBER_ADDRESSES = 20;
	private static final String SCHEME_BITCOIN = "bitcoin:";
	private static final String ADDRESS_DONATE = "17GXYDJEDUqw7hYtqquyN1kYWmtcmFKhK8";
	private Menu menu;
	public String lastSeed = null, temporalSeed = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
					WindowManager.LayoutParams.FLAG_SECURE);
		}
		init(savedInstanceState);
	}

	private void init(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			getFragmentManager().beginTransaction()
					.replace(R.id.container, new PlaceholderFragment())
					.commit();
		}
		lastSeed = null;
		temporalSeed = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		this.menu = menu;
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void showFinalMenuItems(boolean show) {
		this.menu.findItem(R.id.action_check).setVisible(show);
		this.menu.findItem(R.id.action_clear).setVisible(show);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_donate) {
			try {
				Intent intent = new Intent(Intent.ACTION_VIEW);
				intent.setData(Uri.parse(SCHEME_BITCOIN + ADDRESS_DONATE));
				startActivity(intent);
			} catch (Exception e) {
				((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE))
						.setPrimaryClip(ClipData.newPlainText(
								getString(R.string.address), ADDRESS_DONATE));

				Util.showToast(this, R.string.donate_copied);
			}
			return true;
		}
		if (id == R.id.action_clear) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.clear);
			builder.setMessage(R.string.clear_confirm);
			builder.setNegativeButton(R.string.no, null);
			builder.setPositiveButton(R.string.yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					init(null);
				}
			});
			builder.show();
			return true;
		}
		if (id == R.id.action_check) {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.check);
			builder.setMessage(R.string.check_confirm);
			builder.setNegativeButton(R.string.no, null);
			builder.setPositiveButton(R.string.yes, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final String seed = temporalSeed;
					init(null);
					lastSeed = seed;
					Util.showToast(MainActivity.this, R.string.reenter_check,
							Toast.LENGTH_LONG);
				}
			});
			builder.show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		private LayoutInflater inflater;
		private String[] selectedSuits = new String[52];
		private String[] selectedRanks = new String[selectedSuits.length];
		private Drawable[] suitDrawables = new Drawable[5];
		private Drawable[] rankDrawables = new Drawable[14];
		public static final String SUITS[] = new String[] { null, "S", "C",
				"D", "H" };
		public static final String RANKS[] = new String[] { null, "A", "2",
				"3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K" };
		private ViewPager pager, pagerAddresses;
		private EditText password, password2;

		private boolean isItemSelectedEnabled = true;

		private int maxPageSeen = 0;

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			this.inflater = inflater;
			// Suits
			int pos = 0;
			suitDrawables[pos++] = getResources().getDrawable(R.drawable.suit);
			suitDrawables[pos++] = getResources()
					.getDrawable(R.drawable.spades);
			suitDrawables[pos++] = getResources().getDrawable(R.drawable.clubs);
			suitDrawables[pos++] = getResources().getDrawable(
					R.drawable.diamonds);
			suitDrawables[pos++] = getResources()
					.getDrawable(R.drawable.hearts);
			// Ranks
			pos = 0;
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.rank);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_a);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_2);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_3);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_4);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_5);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_6);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_7);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_8);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_9);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_10);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_j);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_q);
			rankDrawables[pos++] = getResources().getDrawable(R.drawable.r_k);

			final View rootView = inflater.inflate(R.layout.fragment_main,
					container, false);

			password = (EditText) rootView.findViewById(R.id.password);
			password2 = (EditText) rootView.findViewById(R.id.password_confirm);
			password.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence arg0, int arg1,
						int arg2, int arg3) {
				}

				@Override
				public void beforeTextChanged(CharSequence arg0, int arg1,
						int arg2, int arg3) {
				}

				@Override
				public void afterTextChanged(Editable text) {
					password2
							.setVisibility(text.toString().length() > 0 ? View.VISIBLE
									: View.GONE);
				}
			});

			pagerAddresses = (ViewPager) rootView
					.findViewById(R.id.vp_addresses);

			pager = (ViewPager) rootView.findViewById(R.id.vp_cards);
			final CardsAdapter adapter = new CardsAdapter();
			pager.setAdapter(adapter);
			pager.setOnPageChangeListener(new OnPageChangeListener() {
				@Override
				public void onPageScrollStateChanged(int page) {
				}

				@Override
				public void onPageScrolled(int page, float arg1, int arg2) {
				}

				@Override
				public void onPageSelected(int page) {
					if (page > maxPageSeen) {
						maxPageSeen = page;
					}

					boolean notSelected = page > 0 && !isCardSelected(page - 1);
					if (notSelected || isReapeatedCard(page - 1, false)) {
						Util.showToast(getActivity(),
								notSelected ? R.string.select_suit_rank
										: R.string.select_unique_card);
						if (0 < page) {
							pager.setCurrentItem(page - 1);
							maxPageSeen = page - 1;
						}
					} else if (page < selectedSuits.length) {
						// Current suit
						String value = selectedSuits[page];
						if (null != value) {
							int selected = positionOf(SUITS, value);
							isItemSelectedEnabled = false;
							adapter.getSpinner(page, false).setSelection(
									selected);
							isItemSelectedEnabled = true;
							Log.d(TAG, "Set suit(" + pager.getCurrentItem()
									+ "): " + selected);
						}

						// Current rank
						value = selectedRanks[page];
						if (null != value) {
							int selected = positionOf(RANKS, value);
							isItemSelectedEnabled = false;
							adapter.getSpinner(page, true).setSelection(
									selected);
							isItemSelectedEnabled = true;
							Log.d(TAG, "Set rank(" + pager.getCurrentItem()
									+ "): " + selected);
						}
					}
				}
			});

			// KeyPair pair = BTCUtils.generateBrainWifKey(false, "abc");
			// address.setText(pair.address);
			// key.setText(pair.privateKey.privateKeyEncoded);

			return rootView;
		}

		private boolean isCardSelected(int page) {
			if (page < 0 || null == selectedRanks[page]
					|| selectedRanks[page].length() == 0
					|| null == selectedSuits[page]
					|| selectedSuits[page].length() == 0) {
				return false;
			}

			return true;
		}

		private int positionOf(String[] array, String item) {
			for (int position = 0; position < array.length; ++position) {
				if (item.equals(array[position])) {
					return position;
				}
			}
			// 0 by default
			return 0;
		}

		private class CardsAdapter extends PagerAdapter {
			private RelativeLayout[] elements;
			private Spinner[] rankSpinners, suitSpinners;

			public CardsAdapter() {
				elements = new RelativeLayout[getCount()];
				rankSpinners = new Spinner[getCount()];
				suitSpinners = new Spinner[getCount()];
			}

			public Spinner getSpinner(int page, boolean isRank) {
				return (isRank ? rankSpinners : suitSpinners)[page];
			}

			@Override
			public int getCount() {
				// Add 1 for the final "Go!" button
				return selectedSuits.length + 1;
			}

			/**
			 * Create the page for the given position. The adapter is
			 * responsible for adding the view to the container given here,
			 * although it only must ensure this is done by the time it returns
			 * from {@link #finishUpdate()}.
			 * 
			 * @param container
			 *            The containing View in which the page will be shown.
			 * @param position
			 *            The page position to be instantiated.
			 * @return Returns an Object representing the new page. This does
			 *         not need to be a View, but can be some other container of
			 *         the page.
			 */
			@Override
			public Object instantiateItem(View container, final int position) {
				View item;

				// Check if element already exists
				if (null != elements[position]) {
					item = elements[position];
				} else if (position == getCount() - 1) {
					// This is the last item
					item = inflater.inflate(R.layout.card_go,
							(ViewGroup) container, false);
					item.findViewById(R.id.btn_go).setOnClickListener(
							new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									final MainActivity mainActivity = (MainActivity) getActivity();

									// Check password
									if (password.getText().toString().length() > 0
											&& !password
													.getText()
													.toString()
													.equals(password2.getText()
															.toString())) {
										Util.showToast(getActivity(),
												R.string.password_missmatch);
										pagerAddresses.setVisibility(View.GONE);
										mainActivity.showFinalMenuItems(false);
									} else {
										pagerAddresses
												.setVisibility(View.VISIBLE);
										pagerAddresses
												.setAdapter(new AddressesAdapter());
										mainActivity.showFinalMenuItems(true);
										if (null == mainActivity.lastSeed) {
											// Store this seed to re-check
											mainActivity.temporalSeed = getSeed(0);
										} else {
											// Check the generated seed with the
											// last one
											int message;
											if (mainActivity.lastSeed
													.equals(getSeed(0))) {
												message = R.string.last_seed_match;
											} else {
												message = R.string.last_seed_missmatch;
											}

											AlertDialog.Builder builder = new AlertDialog.Builder(
													getActivity());
											builder.setTitle(R.string.check);
											builder.setMessage(message);
											builder.setPositiveButton(
													R.string.ok, null);
											builder.show();

										}
									}
								}
							});
				} else {

					RelativeLayout card = (RelativeLayout) inflater.inflate(
							R.layout.card, (ViewGroup) container, false);

					((TextView) card.findViewById(R.id.id_number))
							.setText(getString(R.string.x_of_y, position + 1,
									selectedSuits.length));

					// SUIT
					final Spinner suit = (Spinner) card
							.findViewById(R.id.spin_suit);
					suit.setAdapter(new CardAdapter(suit, false));
					suit.post(new Runnable() {
						public void run() {
							suit.setOnItemSelectedListener(new OnItemSelectedListener() {

								@Override
								public void onItemSelected(
										AdapterView<?> adapterView, View view,
										int position, long id) {
									// Don't run when loading the view
									if (!isItemSelectedEnabled) {
										return;
									}

									final int page = pager.getCurrentItem();
									Log.d(TAG, "Save suit(" + page + "): "
											+ SUITS[position]);
									selectedSuits[page] = SUITS[position];
									if (!isReapeatedCard(page, true)) {
										autoChangePage(page);
									}
								}

								@Override
								public void onNothingSelected(
										AdapterView<?> arg0) {
								}
							});
						}
					});

					// RANK
					final Spinner rank = (Spinner) card
							.findViewById(R.id.spin_rank);
					rank.setAdapter(new CardAdapter(rank, true));
					rank.post(new Runnable() {
						public void run() {
							rank.setOnItemSelectedListener(new OnItemSelectedListener() {

								@Override
								public void onItemSelected(
										AdapterView<?> adapterView, View view,
										int position, long id) {
									// Don't run when loading the view
									if (!isItemSelectedEnabled) {
										return;
									}

									final int page = pager.getCurrentItem();
									Log.d(TAG, "Save rank(" + page + "): "
											+ RANKS[position]);
									selectedRanks[page] = RANKS[position];
									if (!isReapeatedCard(page, true)) {
										autoChangePage(page);
									}
								}

								@Override
								public void onNothingSelected(
										AdapterView<?> arg0) {
								}
							});
						}
					});

					elements[position] = card;
					suitSpinners[position] = suit;
					rankSpinners[position] = rank;
					item = card;
				}
				((ViewPager) container).addView(item, 0);
				return item;
			}

			private void autoChangePage(int page) {
				if (page == maxPageSeen && isCardSelected(page)
						&& page < selectedSuits.length) {
					pager.setCurrentItem(page + 1);
					Log.d(TAG, "Next page");
				}
			}

			/**
			 * Remove a page for the given position. The adapter is responsible
			 * for removing the view from its container, although it only must
			 * ensure this is done by the time it returns from
			 * {@link #finishUpdate()}.
			 * 
			 * @param container
			 *            The containing View from which the page will be
			 *            removed.
			 * @param position
			 *            The page position to be removed.
			 * @param object
			 *            The same object that was returned by
			 *            {@link #instantiateItem(View, int)}.
			 */
			@Override
			public void destroyItem(View collection, int position, Object view) {
				((ViewPager) collection).removeView((View) view);
				elements[position] = null;
			}

			@Override
			public boolean isViewFromObject(View view, Object object) {
				return view == ((View) object);
			}

			/**
			 * Called when the a change in the shown pages has been completed.
			 * At this point you must ensure that all of the pages have actually
			 * been added or removed from the container as appropriate.
			 * 
			 * @param container
			 *            The containing View which is displaying this adapter's
			 *            page views.
			 */
			@Override
			public void finishUpdate(View view) {
			}

			@Override
			public void restoreState(Parcelable parce, ClassLoader load) {
			}

			@Override
			public Parcelable saveState() {
				return null;
			}

			@Override
			public void startUpdate(View view) {
			}

		}

		private boolean isReapeatedCard(int thisPage, boolean showDialog) {
			if (thisPage <= 0 || null == selectedSuits[thisPage]
					|| null == selectedRanks[thisPage]) {
				// We can't check yet
				return false;
			}

			for (int page = 0; page < thisPage; ++page) {
				if (selectedRanks[thisPage].equals(selectedRanks[page])
						&& selectedSuits[thisPage].equals(selectedSuits[page])) {
					// This is a repeated card!
					if (showDialog) {
						final int finalPage = page;
						AlertDialog.Builder builder = new AlertDialog.Builder(
								getActivity());
						builder.setTitle(R.string.repeated_card);
						builder.setMessage(getString(
								R.string.repeated_card_message, page + 1));
						builder.setPositiveButton(
								getString(R.string.go_card, page + 1),
								new OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										pager.setCurrentItem(finalPage);
									}
								});
						builder.setNegativeButton(R.string.stay_here, null);
						builder.show();
					}
					return true;
				}
			}

			return false;
		}

		private class AddressesAdapter extends PagerAdapter {
			private View[] elements;

			public AddressesAdapter() {
				elements = new RelativeLayout[getCount()];
			}

			@Override
			public int getCount() {
				return NUMBER_ADDRESSES;
			}

			/**
			 * Create the page for the given position. The adapter is
			 * responsible for adding the view to the container given here,
			 * although it only must ensure this is done by the time it returns
			 * from {@link #finishUpdate()}.
			 * 
			 * @param container
			 *            The containing View in which the page will be shown.
			 * @param position
			 *            The page position to be instantiated.
			 * @return Returns an Object representing the new page. This does
			 *         not need to be a View, but can be some other container of
			 *         the page.
			 */
			@Override
			public Object instantiateItem(View container, final int position) {
				View item;

				// Check if element already exists
				if (null != elements[position]) {
					item = elements[position];
				} else {
					final View address = inflater.inflate(R.layout.address,
							(ViewGroup) container, false);

					((TextView) address.findViewById(R.id.id_number))
							.setText(getString(R.string.x_of_y, position + 1,
									getCount()));

					new LoadAddress(position, address).execute();

					elements[position] = address;
					item = address;
				}
				((ViewPager) container).addView(item, 0);
				return item;
			}

			class LoadAddress extends AsyncTask<Void, Void, KeyPair> {
				private int page;
				private View address;

				public LoadAddress(int page, View address) {
					this.page = page;
					this.address = address;
				}

				@Override
				protected KeyPair doInBackground(Void... params) {
					return getAddress(page);
				}

				@Override
				protected void onPostExecute(final KeyPair keyPair) {
					super.onPostExecute(keyPair);
					((TextView) address.findViewById(R.id.txt_address))
							.setText(keyPair.address);
					((TextView) address.findViewById(R.id.txt_key))
							.setText(keyPair.privateKey.privateKeyEncoded);
					address.findViewById(R.id.img_address).setOnClickListener(
							new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									showQRCodePopup(keyPair.address, true,
											inflater);
								}
							});
					address.findViewById(R.id.img_key).setOnClickListener(
							new View.OnClickListener() {
								@Override
								public void onClick(View view) {
									showQRCodePopup(
											keyPair.privateKey.privateKeyEncoded,
											false, inflater);
								}
							});
				}
			}

			/**
			 * Remove a page for the given position. The adapter is responsible
			 * for removing the view from its container, although it only must
			 * ensure this is done by the time it returns from
			 * {@link #finishUpdate()}.
			 * 
			 * @param container
			 *            The containing View from which the page will be
			 *            removed.
			 * @param position
			 *            The page position to be removed.
			 * @param object
			 *            The same object that was returned by
			 *            {@link #instantiateItem(View, int)}.
			 */
			@Override
			public void destroyItem(View collection, int position, Object view) {
				((ViewPager) collection).removeView((View) view);
				elements[position] = null;
			}

			@Override
			public boolean isViewFromObject(View view, Object object) {
				return view == ((View) object);
			}

			/**
			 * Called when the a change in the shown pages has been completed.
			 * At this point you must ensure that all of the pages have actually
			 * been added or removed from the container as appropriate.
			 * 
			 * @param container
			 *            The containing View which is displaying this adapter's
			 *            page views.
			 */
			@Override
			public void finishUpdate(View view) {
			}

			@Override
			public void restoreState(Parcelable parce, ClassLoader load) {
			}

			@Override
			public Parcelable saveState() {
				return null;
			}

			@Override
			public void startUpdate(View view) {
			}

			private void showQRCodePopup(final String content,
					final boolean isAddress, final LayoutInflater inflater) {
				DisplayMetrics dm = getResources().getDisplayMetrics();
				final int screenSize = Math
						.min(dm.widthPixels, dm.heightPixels);
				final String uriStr = isAddress ? SCHEME_BITCOIN + content
						: content;
				new AsyncTask<Void, Void, Bitmap>() {

					@Override
					protected Bitmap doInBackground(Void... params) {
						return QRCode.getMinimumQRCode(uriStr,
								ErrorCorrectLevel.M)
								.createImage(screenSize / 2);
					}

					@Override
					protected void onPostExecute(final Bitmap bitmap) {
						if (bitmap != null) {
							View view = inflater.inflate(R.layout.address_qr,
									null);
							if (view != null) {
								final ImageView qrView = (ImageView) view
										.findViewById(R.id.qr_code_image);
								qrView.setImageBitmap(bitmap);

								final TextView bitcoinProtocolLinkView = (TextView) view
										.findViewById(R.id.link1);
								final TextView blockexplorerLinkView = (TextView) view
										.findViewById(R.id.link2);
								final TextView blockchainLinkView = (TextView) view
										.findViewById(R.id.link3);

								if (isAddress) {

									SpannableStringBuilder labelUri = new SpannableStringBuilder(
											uriStr);
									ClickableSpan urlSpan = new ClickableSpan() {
										@Override
										public void onClick(View widget) {
											try {
												Intent intent = new Intent(
														Intent.ACTION_VIEW);
												intent.setData(Uri
														.parse(uriStr));
												startActivity(intent);
											} catch (Exception e) {
												Util.showToast(getActivity(),
														R.string.no_wallet);
											}
										}
									};
									labelUri.setSpan(
											urlSpan,
											0,
											labelUri.length(),
											SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
									bitcoinProtocolLinkView.setText(labelUri);
									bitcoinProtocolLinkView
											.setMovementMethod(LinkMovementMethod
													.getInstance());

									// Link 2
									SpannableStringBuilder blockexplorerLinkText = new SpannableStringBuilder(
											"blockexplorer.com");
									Util.setUrlSpanForAddress(
											"blockexplorer.com", content,
											blockexplorerLinkText);
									blockexplorerLinkView
											.setText(blockexplorerLinkText);
									blockexplorerLinkView
											.setMovementMethod(LinkMovementMethod
													.getInstance());

									// Link 3
									SpannableStringBuilder blockchainLinkText = new SpannableStringBuilder(
											"blockchain.info");
									Util.setUrlSpanForAddress(
											"blockchain.info", content,
											blockchainLinkText);
									blockchainLinkView
											.setText(blockchainLinkText);
									blockchainLinkView
											.setMovementMethod(LinkMovementMethod
													.getInstance());
								} else {
									bitcoinProtocolLinkView
											.setVisibility(View.GONE);
									blockexplorerLinkView
											.setVisibility(View.GONE);
									blockchainLinkView.setVisibility(View.GONE);
								}

								final AlertDialog.Builder builder = new AlertDialog.Builder(
										getActivity());
								final int title = isAddress ? R.string.address
										: R.string.private_key;
								builder.setTitle(title);
								builder.setView(view);
								builder.setNeutralButton(R.string.share,
										new DialogInterface.OnClickListener() {

											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												Intent intent = new Intent(
														Intent.ACTION_SEND);
												intent.setType("text/plain");
												intent.putExtra(
														Intent.EXTRA_SUBJECT,
														getString(title));
												intent.putExtra(
														Intent.EXTRA_TEXT,
														content);
												startActivity(Intent
														.createChooser(
																intent,
																getString(title)));
											}
										});
								if (PrintHelper.systemSupportsPrint()) {
									builder.setPositiveButton(
											R.string.print,
											new DialogInterface.OnClickListener() {
												@Override
												public void onClick(
														DialogInterface dialog,
														int which) {
													Renderer.printQR(
															getActivity(),
															uriStr);
												}
											});
									builder.setNegativeButton(
											android.R.string.cancel, null);
								} else {
									builder.setPositiveButton(
											android.R.string.ok, null);
								}

								builder.show();
							}
						}
					}
				}.execute();
			}

		}

		private String getSeed(int index) {
			String seed = password.getText().toString();

			for (int pos = index; pos < selectedRanks.length; ++pos) {
				seed += selectedRanks[pos] + selectedSuits[pos];
			}

			return seed;
		}

		private KeyPair getAddress(int index) {
			return Util.generateBrainWifKey(false, getSeed(index));
		}

		public class CardAdapter extends BaseAdapter {
			private boolean isRank;

			private class ViewHolder {
				ImageView imageView;
			}

			private Drawable[] getDrawables() {
				return isRank ? rankDrawables : suitDrawables;
			}

			public CardAdapter(Spinner spinner, boolean isRank) {
				this.isRank = isRank;
			}

			@Override
			public int getCount() {
				return getDrawables().length;
			}

			@Override
			public Object getItem(int pos) {
				return null;
			}

			@Override
			public long getItemId(int pos) {
				return 0;
			}

			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				ViewHolder holder;

				if (null == convertView) {
					convertView = inflater.inflate(R.layout.card_suit, parent,
							false);

					holder = new ViewHolder();
					holder.imageView = (ImageView) convertView
							.findViewById(R.id.image_suit);

					convertView.setTag(holder);
				} else {
					holder = (ViewHolder) convertView.getTag();
				}

				holder.imageView.setImageDrawable(getDrawables()[position]);

				return convertView;
			}

		}

	}

}
