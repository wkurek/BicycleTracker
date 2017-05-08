package tracker.hfad.com.bicycletracker;


import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;


public class SettingsFragment extends Fragment {


    public SettingsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public  void onStart()
    {
        super.onStart();

        final View view = getView();

        final Spinner spinner1 = (Spinner) view.findViewById(R.id.settings_spinner_1);
        final Spinner spinner2 = (Spinner) view.findViewById(R.id.settings_spinner_2);

        ArrayList<String> activities_types_list = new ArrayList<String>();
        activities_types_list.add("riding a bike");
        activities_types_list.add("running");

        ArrayList<String> languages_list = new ArrayList<String>();
        languages_list.add("English");


        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(getActivity(), R.layout.spiner_style, activities_types_list);
        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(getActivity(), R.layout.spiner_style, languages_list);

        spinner1.setAdapter(adapter1);
        spinner2.setAdapter(adapter2);

        spinner1.setSelection(SaveSharedPreference.getUserActivity(getActivity()));
        spinner2.setSelection(SaveSharedPreference.getUserLanguage(getActivity()));

        spinner1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                SaveSharedPreference.setUserActivity(getActivity(), position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id)
            {
                SaveSharedPreference.setUserLanguage(getActivity(), position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        Button transferButton = (Button) view.findViewById(R.id.settings_transfer_button);
        transferButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                ExternDatabaseTasks.ImportDataFromServerTask importDataFromServerTask =
                        new ExternDatabaseTasks.ImportDataFromServerTask(getActivity());

                importDataFromServerTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });

    }

}
