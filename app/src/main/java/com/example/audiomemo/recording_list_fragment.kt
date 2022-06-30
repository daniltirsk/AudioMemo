package com.example.audiomemo

import android.Manifest
import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"



/**
 * A simple [Fragment] subclass.
 * Use the [BlankFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RecordingListFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var output: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordingGoing: Boolean = false
    private var recordingPaused: Boolean = false

    lateinit var recordings_save_path: String
//    var path = this.filesDir.absolutePath

    lateinit var fragView: View
    lateinit var progressText: TextView
    lateinit var button_start_recording: ImageView
    lateinit var button_stop_recording: ImageView
    lateinit var keyButton: ImageView
    lateinit var trackList: RecyclerView
    lateinit var adapter: AudioAdapter
    lateinit var importanceSpinner: Spinner
    var allrecordings = mutableListOf<RecordingModel>()
    var recordingsToShow = mutableListOf<RecordingModel>()

    lateinit var curRecording: RecordingModel
    lateinit var dao: RecordingModelDao

    lateinit var runnable: Runnable
    private var handler: Handler = Handler(Looper.getMainLooper())
    var recordingDuration = 0
    var timeLimit = 30

    var searchQuery: CharSequence = ""
    var importanceQuery: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }

    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        fragView = inflater.inflate(R.layout.recordings_list, container, false)


        recordings_save_path = this.requireActivity().getExternalFilesDir("AudioMemo")?.absolutePath!! + "/"

        button_start_recording = fragView.findViewById<ImageView>(R.id.button_start_recording)
        button_stop_recording = fragView.findViewById<ImageView>(R.id.button_stop_recording)

        dao = (this.requireActivity() as MainActivity).db.recordingsDao()

        loadTracks()

        button_start_recording.setOnClickListener {
            if (ContextCompat.checkSelfPermission(fragView.context,
                    Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(fragView.context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(fragView.context,
                    Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
                val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.INTERNET)
                ActivityCompat.requestPermissions(this.requireActivity(), permissions,0)
            } else {
                if (recordingGoing){
                    pauseRecording()
                } else {
                    startRecording()
                }
            }
        }

        button_stop_recording.setOnClickListener{
            stopRecording()
        }

        progressText = fragView.findViewById(R.id.progress)
        keyButton = fragView.findViewById(R.id.key_button)

        keyButton.setOnClickListener {
            ApiKeyDialog(this.context!!).show(this.requireActivity().supportFragmentManager, "choice")
        }

        fragView.findViewById<EditText>(R.id.search_bar).addTextChangedListener(object :
            TextWatcher {
            override fun afterTextChanged(s: Editable?) {

            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.e("my", s.toString())
                if (s != null) {
                    searchQuery = s
                }
                filterTracks()
                adapter.notifyDataSetChanged()
            }
        })

        importanceSpinner = fragView.findViewById(R.id.importance_spinner)

        ArrayAdapter.createFromResource(
            fragView.context,
            R.array.importance_sort_values,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            importanceSpinner.adapter = adapter
        }

        importanceSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                importanceQuery = position
                filterTracks()
                adapter.notifyDataSetChanged()
            }

        }

        return fragView
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BlankFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            RecordingListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private fun startRecordingDurationView() {
        progressText.visibility = View.VISIBLE
        runnable = Runnable {
            if (recordingGoing){
                if (!recordingPaused){
                    recordingDuration++
                    handler.postDelayed(runnable, 1000)
                }
            } else {
                recordingDuration = 0
                progressText.visibility = View.GONE
            }

            if (recordingDuration >= timeLimit){
                Toast.makeText(fragView.context,"Time limit reached!", Toast.LENGTH_SHORT).show()
                stopRecording()
            }

            this.progressText.text = "${recordingDuration / 60}:${recordingDuration % 60 / 10}${recordingDuration % 10}"
        }
        handler.postDelayed(runnable, 1000)
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    private fun startRecording() {
        try {
            val created_at = SimpleDateFormat("dd,MM,yyyy_HH_mm_ss").format(Calendar.getInstance().time)
            val filename = "${created_at}.ogg"

//            var filename = "recording.mp3"
            output = recordings_save_path + filename

            curRecording = RecordingModel(output!!)

            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.OGG)
                setOutputFile(output)
                setAudioEncoder(MediaRecorder.AudioEncoder.OPUS)
                setMaxDuration(timeLimit * 1000)

                try {
                    prepare()
                } catch (e: IOException) {
                    Log.e("mytag", "prepare() failed")
                    Log.e("mytag", e.toString())
                }

                start()
            }
            recordingGoing = true
            Toast.makeText(fragView.context, "Recording started!", Toast.LENGTH_SHORT).show()
            button_start_recording.setBackgroundResource(R.drawable.pause_recording)
            startRecordingDurationView()
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    @TargetApi(Build.VERSION_CODES.N)
    private fun pauseRecording() {
        if(recordingGoing) {
            if(!recordingPaused){
                Toast.makeText(fragView.context,"Stopped!", Toast.LENGTH_SHORT).show()
                mediaRecorder?.pause()
                recordingPaused = true
                button_start_recording.setBackgroundResource(R.drawable.start_recording)
            }else{
                resumeRecording()
            }
        }
    }

    @SuppressLint("RestrictedApi", "SetTextI18n")
    @TargetApi(Build.VERSION_CODES.N)
    private fun resumeRecording() {
        Toast.makeText(fragView.context,"Resume!", Toast.LENGTH_SHORT).show()
        mediaRecorder?.resume()
        button_start_recording.setBackgroundResource(R.drawable.pause_recording)
        recordingPaused = false
        startRecordingDurationView()
    }

    private fun stopRecording(){
        if(recordingGoing){
            mediaRecorder?.stop()
            mediaRecorder?.release()
            recordingGoing = false

            updateTracks()

            GlobalScope.launch {
                dao.insert(curRecording)
                curRecording = dao.getLast()
                GlobalScope.launch(Dispatchers.Main) {
                    var newFrag = RecordingViewFragment.newInstance(curRecording)
                    (fragView.context as MainActivity).supportFragmentManager.beginTransaction().replace(R.id.container_fragm, newFrag).commit()
                }
            }
        }else{
            Toast.makeText(fragView.context, "You are not recording right now!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadTracks() {
        GlobalScope.launch {
            var new_recordings = dao.getAll()
            allrecordings.addAll(new_recordings)
            Log.e("my1", allrecordings.size.toString())
            filterTracks()
        }

        trackList = fragView.findViewById(R.id.recordings_list)
        trackList.layoutManager = LinearLayoutManager(fragView.context)
        adapter = AudioAdapter(fragView.context, recordingsToShow)
        trackList.adapter = adapter
    }

    private fun updateTracks(){
        GlobalScope.launch {
            allrecordings.clear()
            var new_recordings = dao.getAll()
            allrecordings.addAll(new_recordings)
            filterTracks()
        }
        adapter.notifyDataSetChanged()
    }

    private fun filterTracks(){
        recordingsToShow.clear()

            for (rec in allrecordings){
                if (rec.name?.contains(this.searchQuery, ignoreCase = true) == true){
                    if (rec.importance == importanceQuery - 1 || importanceQuery == 0) {
                        recordingsToShow.add(rec)
                    }
                }
            }

        Log.e("my", recordingsToShow.size.toString())
    }
}
