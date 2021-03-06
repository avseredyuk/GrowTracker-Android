package me.anon.grow.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.Html;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.kenny.snackbar.SnackBar;
import com.kenny.snackbar.SnackBarListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import lombok.Setter;
import me.anon.controller.adapter.PlantAdapter;
import me.anon.controller.adapter.SimpleItemTouchHelperCallback;
import me.anon.grow.AddPlantActivity;
import me.anon.grow.AddWateringActivity;
import me.anon.grow.MainActivity;
import me.anon.grow.MainApplication;
import me.anon.grow.R;
import me.anon.lib.Views;
import me.anon.lib.event.GardenChangeEvent;
import me.anon.lib.helper.BusHelper;
import me.anon.lib.helper.FabAnimator;
import me.anon.lib.manager.GardenManager;
import me.anon.lib.manager.PlantManager;
import me.anon.model.EmptyAction;
import me.anon.model.Garden;
import me.anon.model.NoteAction;
import me.anon.model.Plant;
import me.anon.model.PlantStage;

/**
 * // TODO: Add class description
 *
 * @author 7LPdWcaW
 * @documentation // TODO Reference flow doc
 * @project GrowTracker
 */
@Views.Injectable
public class PlantListFragment extends Fragment
{
	private PlantAdapter adapter;
	@Setter private Garden garden;

	public static PlantListFragment newInstance(@Nullable Garden garden)
	{
		PlantListFragment fragment = new PlantListFragment();
		fragment.setGarden(garden);

		return fragment;
	}

	@Views.InjectView(R.id.action_container) private View actionContainer;
	@Views.InjectView(R.id.recycler_view) private RecyclerView recycler;

	private ArrayList<PlantStage> filterList = new ArrayList<>();
	private boolean hideHarvested = false;

	@Override public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setHasOptionsMenu(true);
	}

	@Nullable @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.plant_list_view, container, false);
		Views.inject(this, view);

		return view;
	}

	@Override public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		getActivity().setTitle(garden == null ? "All" : garden.getName() + " plants");

		adapter = new PlantAdapter(getActivity());

		if (MainApplication.isTablet())
		{
			GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), 2);
			RecyclerView.ItemDecoration spacesItemDecoration = new RecyclerView.ItemDecoration()
			{
				private int space = (int)(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics()) / 2f);

				@Override
				public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state)
				{
					if (parent.getPaddingLeft() != space)
					{
						parent.setPadding(space, space, space, space);
						parent.setClipToPadding(false);
					}

					outRect.top = space;
					outRect.bottom = space;
					outRect.left = space;
					outRect.right = space;
				}
			};

			recycler.setLayoutManager(layoutManager);
			recycler.addItemDecoration(spacesItemDecoration);
		}
		else
		{
			recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
		}

		recycler.setAdapter(adapter);

		ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(adapter)
		{
			@Override public boolean isLongPressDragEnabled()
			{
				return filterList.size() == PlantStage.values().length - (hideHarvested ? 1 : 0);
			}
		};
		ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
		touchHelper.attachToRecyclerView(recycler);

		if (garden != null)
		{
			actionContainer.setVisibility(View.VISIBLE);
		}

		filterList.addAll(Arrays.asList(PlantStage.values()));

		if (hideHarvested = PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("hide_harvested", false))
		{
			filterList.remove(PlantStage.HARVESTED);
		}
	}

	@Override public void onStart()
	{
		super.onStart();

		filter();
	}

	@Override public void onStop()
	{
		super.onStop();

		if (filterList.size() == PlantStage.values().length - (hideHarvested ? 1 : 0))
		{
			saveCurrentState();
		}
	}

	private void saveCurrentState()
	{
		ArrayList<Plant> plants = new ArrayList<Plant>();
		ArrayList<String> plantIds = new ArrayList<>();
		plants.addAll(new ArrayList(Arrays.asList(new Plant[adapter.getItemCount()])));
		plantIds.addAll(new ArrayList(Arrays.asList(new String[adapter.getItemCount()])));

		for (Plant plant : PlantManager.getInstance().getPlants())
		{
			int adapterIndex = adapter.getPlants().indexOf(plant);

			if (adapterIndex > -1)
			{
				plants.set(adapterIndex, plant);
				plantIds.set(adapterIndex, plant.getId());
			}
			else
			{
				plants.add(plant);
			}
		}

		if (garden == null)
		{
			PlantManager.getInstance().setPlants(plants);

			new Handler().postDelayed(new Runnable()
			{
				@Override public void run()
				{
					PlantManager.getInstance().save();
				}
			}, 150);
		}
		else
		{
			garden.setPlantIds(plantIds);
			GardenManager.getInstance().save();
		}
	}

	@Views.OnClick public void onFabAddClick(View view)
	{
		Intent addPlant = new Intent(getActivity(), AddPlantActivity.class);

		if (garden != null)
		{
			addPlant.putExtra("garden_index", GardenManager.getInstance().getGardens().indexOf(garden));
		}

		startActivity(addPlant);
	}

	@Views.OnClick public void onFeedingClick(View view)
	{
		int[] plants = new int[adapter.getItemCount()];

		int index = 0;
		for (Plant plant : adapter.getPlants())
		{
			plants[index] = PlantManager.getInstance().getPlants().indexOf(plant);
			index++;
		}

		Intent feed = new Intent(getActivity(), AddWateringActivity.class);
		feed.putExtra("plant_index", plants);
		startActivityForResult(feed, 2);
	}

	@Views.OnClick public void onActionClick(final View view)
	{
		ActionDialogFragment dialogFragment = new ActionDialogFragment();
		dialogFragment.setOnActionSelected(new ActionDialogFragment.OnActionSelected()
		{
			@Override public void onActionSelected(final EmptyAction action)
			{
				for (Plant plant : adapter.getPlants())
				{
					plant.getActions().add(action.clone());
					PlantManager.getInstance().upsert(PlantManager.getInstance().getPlants().indexOf(plant), plant);
				}

				SnackBar.show(getActivity(), "Actions added", new SnackBarListener()
				{
					@Override public void onSnackBarStarted(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateUp(getView().findViewById(R.id.fab_add));
						}
					}

					@Override public void onSnackBarFinished(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateDown(getView().findViewById(R.id.fab_add));
						}
					}

					@Override public void onSnackBarAction(Object o)
					{
					}
				});
			}
		});
		dialogFragment.show(getFragmentManager(), null);
	}

	@Views.OnClick public void onNoteClick(final View view)
	{
		NoteDialogFragment dialogFragment = new NoteDialogFragment();
		dialogFragment.setOnDialogConfirmed(new NoteDialogFragment.OnDialogConfirmed()
		{
			@Override public void onDialogConfirmed(String notes)
			{
				for (Plant plant : adapter.getPlants())
				{
					NoteAction action = new NoteAction(notes);
					plant.getActions().add(action);
					PlantManager.getInstance().upsert(PlantManager.getInstance().getPlants().indexOf(plant), plant);
				}

				SnackBar.show(getActivity(), "Notes added", new SnackBarListener()
				{
					@Override public void onSnackBarStarted(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateUp(getView().findViewById(R.id.fab_add));
						}
					}

					@Override public void onSnackBarFinished(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateDown(getView().findViewById(R.id.fab_add));
						}
					}

					@Override public void onSnackBarAction(Object o)
					{
					}
				});
			}
		});
		dialogFragment.show(getFragmentManager(), null);
	}

	@Override public void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		if (requestCode == 2)
		{
			if (resultCode != Activity.RESULT_CANCELED)
			{
				SnackBar.show(getActivity(), "Watering added", new SnackBarListener()
				{
					@Override public void onSnackBarStarted(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateUp(getView().findViewById(R.id.fab_add));
						}
					}

					@Override public void onSnackBarAction(Object object)
					{

					}

					@Override public void onSnackBarFinished(Object o)
					{
						if (getView() != null)
						{
							FabAnimator.animateDown(getView().findViewById(R.id.fab_add));
						}
					}
				});
			}
		}

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		inflater.inflate(R.menu.plant_list_menu, menu);

		if (garden != null)
		{
			menu.findItem(R.id.edit_garden).setVisible(true);
			menu.findItem(R.id.delete_garden).setVisible(true);
		}

		if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getBoolean("hide_harvested", false))
		{
			menu.findItem(R.id.filter_harvested).setVisible(false);
		}

		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override public boolean onOptionsItemSelected(MenuItem item)
	{
		if (item.getItemId() == R.id.edit_garden)
		{
			GardenDialogFragment dialogFragment = new GardenDialogFragment(garden);
			dialogFragment.setOnEditGardenListener(new GardenDialogFragment.OnEditGardenListener()
			{
				@Override public void onGardenEdited(Garden garden)
				{
					int index = GardenManager.getInstance().getGardens().indexOf(PlantListFragment.this.garden);
					GardenManager.getInstance().getGardens().set(index, garden);
					GardenManager.getInstance().save();
					PlantListFragment.this.garden = garden;

					getActivity().setTitle(garden == null ? "All" : garden.getName() + " plants");
					filter();

					((MainActivity)getActivity()).setNavigationView();
				}
			});
			dialogFragment.show(getFragmentManager(), null);

			return true;
		}
		else if (item.getItemId() == R.id.delete_garden)
		{
			new AlertDialog.Builder(getActivity())
				.setTitle("Are you sure?")
				.setMessage(Html.fromHtml("Are you sure you want to delete garden <b>" + garden.getName() + "</b>? This will not delete the plants."))
				.setPositiveButton("Yes", new DialogInterface.OnClickListener()
				{
					@Override public void onClick(DialogInterface dialogInterface, int i)
					{
						final Garden oldGarden = garden;
						final int oldIndex = GardenManager.getInstance().getGardens().indexOf(garden);

						GardenManager.getInstance().getGardens().remove(garden);
						GardenManager.getInstance().save();

						SnackBar.show(getActivity(), "Garden deleted", "undo", new SnackBarListener()
						{
							@Override public void onSnackBarStarted(Object o){}
							@Override public void onSnackBarFinished(Object o){}

							@Override public void onSnackBarAction(Object o)
							{
								GardenManager.getInstance().getGardens().add(oldIndex, oldGarden);
								GardenManager.getInstance().save();

								BusHelper.getInstance().post(new GardenChangeEvent());
							}
						});

						((MainActivity)getActivity()).setNavigationView();
						((MainActivity)getActivity()).getNavigation().getMenu().findItem(R.id.all).setChecked(true);
						((MainActivity)getActivity()).onNavigationItemSelected(((MainActivity)getActivity()).getNavigation().getMenu().findItem(R.id.all));
					}
				})
				.setNegativeButton("No", null)
				.show();
		}
		else
		{
			if (item.isCheckable())
			{
				item.setChecked(!item.isChecked());
			}

			boolean filter = false;

			if (filterList.size() == PlantStage.values().length - (hideHarvested ? 1 : 0))
			{
				saveCurrentState();
			}

			if (item.getItemId() == R.id.filter_germination)
			{
				if (filterList.contains(PlantStage.GERMINATION))
				{
					filterList.remove(PlantStage.GERMINATION);
				}
				else
				{
					filterList.add(PlantStage.GERMINATION);
				}

				filter = true;
			}
			else if (item.getItemId() == R.id.filter_vegetation)
			{
				if (filterList.contains(PlantStage.VEGETATION))
				{
					filterList.remove(PlantStage.VEGETATION);
				}
				else
				{
					filterList.add(PlantStage.VEGETATION);
				}

				filter = true;
			}
			else if (item.getItemId() == R.id.filter_flowering)
			{
				if (filterList.contains(PlantStage.FLOWER))
				{
					filterList.remove(PlantStage.FLOWER);
				}
				else
				{
					filterList.add(PlantStage.FLOWER);
				}

				filter = true;
			}
			else if (item.getItemId() == R.id.filter_drying)
			{
				if (filterList.contains(PlantStage.DRYING))
				{
					filterList.remove(PlantStage.DRYING);
				}
				else
				{
					filterList.add(PlantStage.DRYING);
				}

				filter = true;
			}
			else if (item.getItemId() == R.id.filter_curing)
			{
				if (filterList.contains(PlantStage.CURING))
				{
					filterList.remove(PlantStage.CURING);
				}
				else
				{
					filterList.add(PlantStage.CURING);
				}

				filter = true;
			}
			else if (item.getItemId() == R.id.filter_harvested)
			{
				if (filterList.contains(PlantStage.HARVESTED))
				{
					filterList.remove(PlantStage.HARVESTED);
				}
				else
				{
					filterList.add(PlantStage.HARVESTED);
				}

				filter = true;
			}

			if (filter)
			{
				filter();
			}
		}

		return super.onOptionsItemSelected(item);
	}

	private void filter()
	{
		ArrayList<Plant> plants = new ArrayList<>();
		plants.addAll(PlantManager.getInstance().getSortedPlantList(garden));

		for (int index = 0; index < plants.size(); index++)
		{
			if (!filterList.contains(plants.get(index).getStage()))
			{
				plants.set(index, null);
			}
		}

		plants.removeAll(Collections.singleton(null));
		adapter.setPlants(plants);
		adapter.notifyDataSetChanged();
	}
}
