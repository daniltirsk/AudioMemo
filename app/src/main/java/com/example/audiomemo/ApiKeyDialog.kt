package com.example.audiomemo
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.audiomemo.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

class ApiKeyDialog(val ctx: Context): DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        var choice = 0
        var dao: ApiKeyDao = (ctx as MainActivity).db.apiKeyDao()
        return ctx.let {
            val builder = AlertDialog.Builder(it)
            builder.setTitle("API KEY")
            val input = EditText(this.context)
            builder.setView(input)
            input.setHint("Enter API KEY")

            GlobalScope.launch {
                if (dao.getAll().size != 0){
                    var formerKey = dao.getLast()
                    input.setText(formerKey.key)
                }
            }

            builder.setPositiveButton("OK", DialogInterface.OnClickListener { dialog, which ->
                var newKey: ApiKey = ApiKey(0, input.text.toString())
                GlobalScope.launch {
                    if (newKey.key.trim().length > 0){
                        if (dao.getAll().size == 0){
                            dao.insert(newKey)
                        } else {
                            var formerKey = dao.getLast()
                            formerKey.key = newKey.key
                            dao.update(formerKey)
                        }
                    } else {
                        GlobalScope.launch (Dispatchers.Main) {
                            Toast.makeText(ctx, getString(R.string.validKeyPrompt), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
            builder.setNegativeButton("Cancel", DialogInterface.OnClickListener { dialog, which -> dialog.cancel() })

            builder.create()
        }
    }
}
