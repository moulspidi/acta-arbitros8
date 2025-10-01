package com.tonkar.volleyballreferee.ui.game.sanction;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.tonkar.volleyballreferee.R;
import com.tonkar.volleyballreferee.engine.game.IGame;
import com.tonkar.volleyballreferee.engine.game.sanction.SanctionType;
import com.tonkar.volleyballreferee.engine.team.TeamType;

/**
 * Fragment para registrar una Solicitud Improcedente (IR).
 * NO introduce un nuevo SanctionType; mapea IR a la siguiente sanción de retraso
 * (DELAY_WARNING o DELAY_PENALTY) según el historial del equipo.
 */
public class ImproperRequestSanctionSelectionFragment extends Fragment {

    private SanctionSelectionDialogFragment mSanctionSelectionDialogFragment;
    private IGame mGame;
    private @Nullable SanctionType mSelected;

    public static ImproperRequestSanctionSelectionFragment newInstance(@NonNull TeamType teamType) {
        ImproperRequestSanctionSelectionFragment fragment = new ImproperRequestSanctionSelectionFragment();
        Bundle args = new Bundle();
        args.putString("teamType", teamType.name());
        fragment.setArguments(args);
        return fragment;
    }

    /** Llamado por el padre para inyectar dependencias (mismo patrón que otros fragments). */
    void init(@NonNull SanctionSelectionDialogFragment parent, @NonNull IGame game) {
        mSanctionSelectionDialogFragment = parent;
        mGame = game;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Obtenemos el equipo del Bundle (mismo enfoque que DelaySanctionSelectionFragment)
        TeamType teamType = TeamType.valueOf(requireArguments().getString("teamType"));

        // IR -> determinar automáticamente la sanción de retraso que corresponde
        mSelected = mGame.getPossibleDelaySanction(teamType);

        // Usamos el tab existente de "Delay" para habilitar el botón OK en el diálogo
        if (mSanctionSelectionDialogFragment != null) {
            mSanctionSelectionDialogFragment.computeOkAvailability(R.id.delay_sanction_tab);
        }

        // Este layout puede ser minimalista; si ya tenías un layout específico, se mantiene el id.
        // Debe existir res/layout/improper_request_sanction_selection.xml en tu proyecto.
        return inflater.inflate(R.layout.improper_request_sanction_selection, container, false);
    }

    /** Devuelve la sanción calculada (DELAY_WARNING o DELAY_PENALTY). */
    @Nullable
    SanctionType getSelectedSanction() {
        return mSelected;
    }
}
