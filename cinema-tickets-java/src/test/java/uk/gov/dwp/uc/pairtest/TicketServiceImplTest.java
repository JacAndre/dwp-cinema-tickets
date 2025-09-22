package uk.gov.dwp.uc.pairtest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import thirdparty.paymentgateway.TicketPaymentService;
import thirdparty.seatbooking.SeatReservationService;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidPurchaseException;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest.Type.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceImplTest {
    @Mock
    private TicketPaymentService ticketPaymentService;
    @Mock
    private SeatReservationService seatReservationService;
    @InjectMocks
    private TicketServiceImpl ticketService;

    @ParameterizedTest
    @MethodSource("invalidPurchaseRequests")
    void validatePurchaseRequest_shouldThrowException_whenInputVariables_areInvalid(Long accountId, TicketTypeRequest[] requests, String expectedMessage) {
        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(accountId, requests));
        assertTrue(exception.getMessage().contains(expectedMessage));
    }

    @Test
    void validatePurchaseRequest_shouldThrowException_whenChild_withoutAdult() {
        Long accountId = 1L;
        TicketTypeRequest request = childTicket(1);

        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(accountId, request));
        assertTrue(exception.getMessage().contains("Child and Infant tickets must be purchased with an Adult ticket"));
    }

    @Test
    void validatePurchaseRequest_shouldThrowException_whenInfant_withoutAdult() {
        Long accountId = 1L;
        TicketTypeRequest request = infantTicket(1);

        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(accountId, request));
        assertTrue(exception.getMessage().contains("Child and Infant tickets must be purchased with an Adult ticket"));
    }

    @Test
    void validatePurchaseRequest_shouldThrowException_whenInfant_exceedsAdult() {
        Long accountId = 1L;
        TicketTypeRequest[] requests = new TicketTypeRequest[] { adultTicket(1), infantTicket(2) };

        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(accountId, requests));
        assertTrue(exception.getMessage().contains("Each Infant must be accompanied by an Adult"));
    }

    @Test
    void validatePurchaseRequest_shouldThrowException_whenTicketType_isNull() {
        Long accountId = 1L;
        TicketTypeRequest request = new TicketTypeRequest(null, 1);

        InvalidPurchaseException exception = assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(accountId, request));
        assertTrue(exception.getMessage().contains("Ticket type cannot be null"));
    }

    @Test
    void purchaseTickets_shouldCallPaymentAndReservation_whenRequestIsValid() {
        Long accountId = 1L;
        TicketTypeRequest request = adultTicket(1);

        ticketService.purchaseTickets(accountId, request);

        verify(ticketPaymentService).makePayment(accountId, ADULT.getPrice());
        verify(seatReservationService).reserveSeat(accountId, 1);
    }

    @Test
    void purchaseTickets_shouldNotCallPaymentAndReservation_whenRequestIsInvalid() {
        Long accountId = 1L;
        TicketTypeRequest request = new TicketTypeRequest(null, 0);

        assertThrows(InvalidPurchaseException.class, () -> ticketService.purchaseTickets(accountId, request));

        verifyNoInteractions(ticketPaymentService);
        verifyNoInteractions(seatReservationService);
    }

    @Test
    void purchaseTickets_shouldCallPaymentAndReservation_whenExactly25TicketsRequested() {
        Long accountId = 1L;
        TicketTypeRequest request = adultTicket(25);

        ticketService.purchaseTickets(accountId, request);

        verify(ticketPaymentService).makePayment(accountId, 25 * ADULT.getPrice());
        verify(seatReservationService).reserveSeat(accountId, 25);
    }

    @ParameterizedTest
    @MethodSource("validPurchaseRequests")
    void purchaseTickets_shouldCallPaymentAndReservation_forValidRequests(Long accountId, TicketTypeRequest[] requests, int cost, int seats) {
        ticketService.purchaseTickets(accountId, requests);

        verify(ticketPaymentService).makePayment(accountId, cost);
        verify(seatReservationService).reserveSeat(accountId, seats);
    }

    private static Stream<Arguments> invalidPurchaseRequests() {
        return Stream.of(
                Arguments.of(null, new TicketTypeRequest[] { adultTicket(1) }, "Invalid account ID"),
                Arguments.of(-1L, new TicketTypeRequest[] { adultTicket(1) }, "Invalid account ID"),
                Arguments.of(1L, null, "At least one ticket must be requested for purchase"),
                Arguments.of(1L, new TicketTypeRequest[] {}, "At least one ticket must be requested for purchase"),
                Arguments.of(1L, new TicketTypeRequest[] { null }, "Ticket request cannot be null"),
                Arguments.of(1L, new TicketTypeRequest[] { adultTicket(26) }, "Maximum number of tickets exceeded")
        );
    }

    private static Stream<Arguments> validPurchaseRequests() {
        return Stream.of(
                Arguments.of(1L, new TicketTypeRequest[] { adultTicket(2), childTicket(2), infantTicket(1) }, 80, 4),
                Arguments.of(1L, new TicketTypeRequest[] { adultTicket(1), childTicket(1), infantTicket(1) }, 40, 2),
                Arguments.of(1L, new TicketTypeRequest[] { adultTicket(2), infantTicket(2) }, 50, 2),
                Arguments.of(1L, new TicketTypeRequest[] { adultTicket(10), childTicket(10), infantTicket(5) }, 400, 20)
        );
    }

    private static TicketTypeRequest adultTicket(int noOfTickets) {
        return new TicketTypeRequest(ADULT, noOfTickets);
    }

    private static TicketTypeRequest childTicket(int noOfTickets) {
        return new TicketTypeRequest(CHILD, noOfTickets);
    }

    private static TicketTypeRequest infantTicket(int noOfTickets) {
        return new TicketTypeRequest(INFANT, noOfTickets);
    }

}