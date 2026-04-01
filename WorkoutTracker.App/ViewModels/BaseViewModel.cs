using CommunityToolkit.Mvvm.ComponentModel;

namespace WorkoutTracker.App.ViewModels;

public abstract partial class BaseViewModel : ObservableObject
{
    [ObservableProperty]
    private bool isBusy;

    [ObservableProperty]
    private string title = string.Empty;

    [ObservableProperty]
    private string statusMessage = string.Empty;

    protected async Task RunBusyAsync(Func<Task> action, string? successMessage = null)
    {
        if (IsBusy)
        {
            return;
        }

        try
        {
            IsBusy = true;
            StatusMessage = string.Empty;
            await action().ConfigureAwait(false);
            if (!string.IsNullOrWhiteSpace(successMessage))
            {
                StatusMessage = successMessage;
            }
        }
        catch (Exception ex)
        {
            StatusMessage = ex.Message;
        }
        finally
        {
            IsBusy = false;
        }
    }
}
