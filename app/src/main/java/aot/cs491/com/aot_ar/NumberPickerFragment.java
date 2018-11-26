package aot.cs491.com.aot_ar;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.widget.NumberPicker;

public class NumberPickerFragment extends DialogFragment {
    private NumberPicker.OnValueChangeListener valueChangeListener;
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        final NumberPicker numberPicker = new NumberPicker(getActivity());

        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(24);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Hour");

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                valueChangeListener.onValueChange(numberPicker,
                        numberPicker.getValue(), numberPicker.getValue());
            }
        });

//        builder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                valueChangeListener.onValueChange(numberPicker,
//                        numberPicker.getValue(), numberPicker.getValue());
//            }
//        });

        builder.setView(numberPicker);
        return builder.create();
    }
    public NumberPicker.OnValueChangeListener getValueChangeListener() {
        return valueChangeListener;
    }

    public void setValueChangeListener(NumberPicker.OnValueChangeListener valueChangeListener) {
        this.valueChangeListener = valueChangeListener;
    }
}
