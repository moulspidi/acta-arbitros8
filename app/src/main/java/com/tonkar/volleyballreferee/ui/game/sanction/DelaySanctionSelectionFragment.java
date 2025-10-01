package com.tonkar.volleyballreferee.ui.game.sanction;

import android.os.Bundle;
import android.view.*;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import android.widget.Toast;

import com.tonkar.volleyballreferee.R;
import com.tonkar.volleyballreferee.engine.game.IGame;
import com.tonkar.volleyballreferee.engine.api.model.SanctionDto;
import com.tonkar.volleyballreferee.engine.game.sanction.SanctionType;
import com.tonkar.volleyballreferee.engine.team.TeamType;
import com.tonkar.volleyballreferee.ui.team.PlayerToggleButton;
import com.tonkar.volleyballreferee.ui.util.UiUtils;

public class DelaySanctionSelectionFragment extends Fragment {

    private SanctionSelectionDialogFragment mSanctionSelectionDialogFragment;
    private IGame                           mGame;
    private PlayerToggleButton              mDelayWarningButton;
    private PlayerToggleButton              mDelayPenaltyButton;
    private SanctionType                    mSelectedDelaySanction;

    public static DelaySanctionSelectionFragment newInstance(TeamType teamType) {
        DelaySanctionSelectionFragment fragment = new DelaySanctionSelectionFragment();
        Bundle args = new Bundle();
        args.putString("teamType", teamType.toString());
        fragment.setArguments(args);
        return fragment;
    }

    public DelaySanctionSelectionFragment() {}

    void init(SanctionSelectionDialogFragment sanctionSelectionDialogFragment, IGame game) {
        mSanctionSelectionDialogFragment = sanctionSelectionDialogFragment;
        mGame = game;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        TeamType teamType = TeamType.valueOf(requireArguments().getString("teamType"));

        View view = inflater.inflate(R.layout.fragment_delay_sanction_selection, container, false);

        if (mGame != null) {
            mDelayWarningButton = view.findViewById(R.id.delay_warning_button);
            mDelayPenaltyButton = view.findViewById(R.id.delay_penalty_button);

            mDelayWarningButton.setColor(getContext(), mGame.getTeamColor(teamType));
            mDelayPenaltyButton.setColor(getContext(), mGame.getTeamColor(teamType));

            mDelayWarningButton.addOnCheckedChangeListener((cButton, isChecked) -> {
                UiUtils.animate(getContext(), cButton);
                if (isChecked) {
                    mSelectedDelaySanction = SanctionType.DELAY_WARNING;
                    mDelayPenaltyButton.setChecked(false);
                    mSanctionSelectionDialogFragment.computeOkAvailability(R.id.delay_sanction_tab);
                }
            });

            mDelayPenaltyButton.addOnCheckedChangeListener((cButton, isChecked) -> {
                UiUtils.animate(getContext(), cButton);
                if (isChecked) {
                    mSelectedDelaySanction = SanctionType.DELAY_PENALTY;
                    mDelayWarningButton.setChecked(false);
                    mSanctionSelectionDialogFragment.computeOkAvailability(R.id.delay_sanction_tab);
                }
            });

            SanctionType possibleDelaySanction = mGame.getPossibleDelaySanction(teamType);

            ViewGroup delayWarningLayout = view.findViewById(R.id.delay_warning_layout);
            ViewGroup delayPenaltyLayout = view.findViewById(R.id.delay_penalty_layout);
            delayWarningLayout.setVisibility(SanctionType.DELAY_WARNING.equals(possibleDelaySanction) ? View.VISIBLE : View.GONE);
            delayPenaltyLayout.setVisibility(SanctionType.DELAY_PENALTY.equals(possibleDelaySanction) ? View.VISIBLE : View.GONE);
        }

        mSelectedDelaySanction = null;
        mSanctionSelectionDialogFragment.computeOkAvailability(R.id.delay_sanction_tab);

        // Improper Request (IR): map to next delay sanction (warning or penalty)
        View improperRequest = view.findViewById(R.id.btn_improper_request);
        if (improperRequest != null) {
            improperRequest.setOnClickListener(v -> {
                TeamType team = teamType;
                // Decide next delay sanction based on history
                SanctionType next = SanctionType.DELAY_WARNING;
                mGame.addImproperRequest(team);
                Toast.makeText(requireContext(), getString(R.string.improper_request_recorded, next.name()), Toast.LENGTH_SHORT).show();
                mSanctionSelectionDialogFragment.dismiss();
            });
        }

        return view;
    }

    SanctionType getSelectedDelaySanction() {
        return mSelectedDelaySanction;
    }
}
