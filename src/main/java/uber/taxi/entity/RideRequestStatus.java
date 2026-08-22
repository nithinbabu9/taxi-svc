package uber.taxi.entity;

@SuppressWarnings("unused") // COMPLETED is part of the persisted lifecycle and reserved for the completion workflow.
public enum RideRequestStatus {
    OPEN,
    MATCHED,
    CANCELLED,
    COMPLETED
}
