package com.example.audiomemo

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import com.example.audiomemo.databinding.RecordingViewBinding
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.jar.Manifest

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val DATA_PARAM = "recording_data"


class RecordingViewFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var recordingData: RecordingModel? = null
    lateinit var btnplay: ImageView
    lateinit var seekBar: SeekBar
    lateinit var progressText: TextView
    lateinit var delete_button: ImageView
    lateinit var finish_button: ImageView
    lateinit var recordingDao: RecordingModelDao
    lateinit var keyDao: ApiKeyDao
    lateinit var curView: View
    lateinit var importanceSpinner: Spinner
    lateinit var audioPlayer: AudioPlayer
    private var colors = arrayOf(R.color.trivial,R.color.normal,R.color.important)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recordingData = it.getParcelable<RecordingModel>(DATA_PARAM)
        }

    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        recordingDao = (this.context as MainActivity).db.recordingsDao()
        keyDao = (this.context as MainActivity).db.apiKeyDao()

        var bindingRecording  = RecordingViewBinding.inflate(inflater, container, false)

        curView = bindingRecording.root

        bindingRecording.note = recordingData

        importanceSpinner = curView.findViewById(R.id.importance_spinner)

        ArrayAdapter.createFromResource(
            curView.context,
            R.array.importance_values,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            importanceSpinner.adapter = adapter
        }

        importanceSpinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {

            }

            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                curView.setBackgroundColor(curView.context.getColor(colors[position]))
            }

        }

        curView.setBackgroundColor(curView.context.getColor(colors[recordingData!!.importance!!]))


        delete_button = curView.findViewById(R.id.delete_recording)

        delete_button.setOnClickListener {
            GlobalScope.launch {
                deleteRecording(curView)
                goBack(curView)
            }
        }

        finish_button = curView.findViewById(R.id.finish_button)

        finish_button.setOnClickListener {
            GlobalScope.launch {
                if (saveChanges(curView)){
                    goBack(curView)
                }
            }
        }

        btnplay = curView.findViewById(R.id.play_button)
        seekBar = curView.findViewById(R.id.seekBar)
        progressText = curView.findViewById(R.id.progress)

        audioPlayer = AudioPlayer(recordingData!!, btnplay, seekBar, progressText, null)

        btnplay.setOnClickListener {
            if (audioPlayer.isPlaying){
                audioPlayer.stopAudio()
            } else {
                audioPlayer.playAudio()
            }
        }

        curView.findViewById<EditText>(R.id.recording_text).setOnLongClickListener {
            it.isFocusable = true
            it.isFocusableInTouchMode = true
            it.isClickable = true
            Toast.makeText(it.context, "Edit", Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }

        curView.findViewById<EditText>(R.id.nameView).setOnLongClickListener {
            it.isFocusable = true
            it.isFocusableInTouchMode = true
            it.isClickable = true
            Toast.makeText(it.context, "Edit", Toast.LENGTH_SHORT).show()
            return@setOnLongClickListener true
        }

        curView.findViewById<ImageView>(R.id.speech_to_text).setOnClickListener {

            var apiKey: ApiKey

            GlobalScope.launch (Dispatchers.IO) {
                if (keyDao.getAll().size != 0){
                    apiKey = keyDao.getLast()
                    GlobalScope.launch (Dispatchers.IO) {
                        GlobalScope.launch (Dispatchers.Main){
                            Toast.makeText(curView.context, getString(R.string.transcribingAudioNotice), Toast.LENGTH_SHORT).show()
                        }
                        var text = getAudioTranscript(recordingData!!.path!!, apiKey.key)
                        curView.findViewById<EditText>(R.id.recording_text).setText(text)
                    }
                } else {
                    GlobalScope.launch (Dispatchers.Main){
                        Toast.makeText(curView.context, getString(R.string.ApiKeyIsNotSet), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

//        val callback = object : OnBackPressedCallback(true) {
//            override fun handleOnBackPressed() {
//                if (true) {
//                    // your fragment onBackPressed() behavior
//                    GlobalScope.launch {
//                        goBack(curView)
//                    }
//                } else {
//                    // default onBackPressed() behavior
//                    isEnabled = false // DON'T FORGET THIS!
//                    requireActivity().onBackPressedDispatcher.onBackPressed()
//                }
//            }
//        }
//
//        this.requireActivity().onBackPressedDispatcher.addCallback(callback)

        return curView
    }

    fun goBack(view: View) {
        var newFrag = RecordingListFragment()
        (view.context as MainActivity).supportFragmentManager.beginTransaction().replace(R.id.container_fragm, newFrag).commit()
    }

    fun saveChanges(view: View): Boolean{
        recordingData?.name = curView.findViewById<EditText>(R.id.nameView).text.toString()
        recordingData?.text = curView.findViewById<EditText>(R.id.recording_text).text.toString()
        recordingData?.updated_at = Calendar.getInstance().timeInMillis
        recordingData?.importance = curView.findViewById<Spinner>(R.id.importance_spinner).selectedItemPosition

        if (recordingData?.name.toString().trim() != ""){
            recordingDao.update(recordingData!!)
            return true
        } else {
            GlobalScope.launch(Dispatchers.Main) {
                Toast.makeText(view.context,getString(R.string.EnterNamePrompt), Toast.LENGTH_SHORT).show()
            }
            return false
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun deleteRecording(view: View){
        val path = Paths.get(recordingData?.path)

        try {
            Files.deleteIfExists(path)
            recordingDao.delete(recordingData!!)
            goBack(view)
        } catch (e: IOException) {
            Log.e("deletion","Deletion failed.")
            Log.e("deletion",e.toString())
        }
    }


    fun getAudioTranscript(path: String, API_KEY: String): String {
        var result = ""


        var data = File(path).readBytes()
        val yandexUrl = "https://stt.api.cloud.yandex.net/speech/v1/stt:recognize?topic=general&lang=ru-RU"
        val url = URL(yandexUrl)
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = "POST"
        connection.connectTimeout = 300000
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Api-Key ${API_KEY}")

        try {
            val outputStream: DataOutputStream = DataOutputStream(connection.outputStream)
            outputStream.write(data)
            outputStream.flush()
        } catch (exception: Exception) {
            Log.e("My", exception.toString())
            Log.e("My","Exception sending")
        }

        Log.e("my", connection.responseCode.toString())
        Log.e("my", connection.responseMessage.toString())


        if (connection.responseCode == 200) {
            val stream: InputStream
            try {
                stream = connection.getContent() as InputStream
            } catch (exception: Exception) {
                return getString(R.string.ApiRequestErrorMessage)
            }

            val data = Scanner(stream).nextLine()
            Log.d("mytag", data)
            val parser = JsonParser.parseString(data).asJsonObject
            result = parser.get("result").asString
        }

        return result
    }


    companion object {
        @JvmStatic
        fun newInstance(recordingData: RecordingModel) =
            RecordingViewFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(DATA_PARAM, recordingData)
                }
            }
    }
}
